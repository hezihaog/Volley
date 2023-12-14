package com.zh.android.volley.volley;

import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpStack;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.ProtocolVersion;
import cz.msebera.android.httpclient.entity.BasicHttpEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.message.BasicHttpResponse;
import cz.msebera.android.httpclient.message.BasicStatusLine;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.ApplicationProtocolNames;

import org.asynchttpclient.*;
import org.asynchttpclient.netty.NettyResponse;
import org.asynchttpclient.util.HttpConstants;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * AsyncHttpClient实现的Volley网络层
 */
public class AsyncHttpClientStack implements HttpStack {
    private static final int maxRequestRetry = 2;

    private final AsyncHttpClient mAsyncHttpClient;

    public AsyncHttpClientStack() {
        this(Dsl.asyncHttpClient(getConfig(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS)));
    }

    public AsyncHttpClientStack(AsyncHttpClient asyncHttpClient) {
        if (asyncHttpClient == null) {
            throw new IllegalArgumentException("AsyncHttpClient can't be null");
        }
        this.mAsyncHttpClient = asyncHttpClient;
    }

    private static AsyncHttpClientConfig getConfig(int timeout) {
        EventLoopGroup eventLoopGroup;
        // Linux平台特有，判断Epoll是否可用，可用则使用Epoll，Epoll的事件驱动模型更加高效
        if (Epoll.isAvailable()) {
            eventLoopGroup = new EpollEventLoopGroup();
        } else {
            eventLoopGroup = new NioEventLoopGroup();
        }
        return new DefaultAsyncHttpClientConfig
                .Builder()
                .setConnectTimeout(timeout)
                .setRequestTimeout(timeout)
                .setReadTimeout(timeout)
                .setEventLoopGroup(eventLoopGroup)
                // 设置信任所有ssl
                .setUseInsecureTrustManager(true)
                .setMaxRequestRetry(maxRequestRetry)
                .build();
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        int timeoutMs = request.getTimeoutMs();

        AsyncHttpClient client;
        //3个超时时间都不一样时，重新构建一个AsyncHttpClient，才可以设置
        if (timeoutMs != mAsyncHttpClient.getConfig().getConnectTimeout() &&
                timeoutMs != mAsyncHttpClient.getConfig().getReadTimeout() &&
                timeoutMs != mAsyncHttpClient.getConfig().getRequestTimeout()) {
            client = Dsl.asyncHttpClient(getConfig(timeoutMs));
        } else {
            client = mAsyncHttpClient;
        }

        //创建AsyncHttpClient的请求
        RequestBuilder builder = new RequestBuilder();
        //设置请求Url
        builder.setUrl(request.getUrl());

        //添加请求Header
        Map<String, String> headers = request.getHeaders();
        HttpHeaders defaultHeaders = new DefaultHttpHeaders();
        for (String name : headers.keySet()) {
            defaultHeaders.add(name, headers.get(name));
        }
        for (String name : additionalHeaders.keySet()) {
            defaultHeaders.add(name, additionalHeaders.get(name));
        }
        builder.setHeaders(defaultHeaders);

        //设置请求的参数
        setConnectionParametersForRequest(builder, request);

        //构建请求
        org.asynchttpclient.Request clientRequest = builder.build();

        //发起同步请求，获取响应
        NettyResponse response;
        try {
            response = (NettyResponse) client.prepareRequest(clientRequest).execute().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //获取协议
        String protocol = getProtocol(response);

        //转换相应状态行
        BasicStatusLine responseStatus = new BasicStatusLine(
                //把AsyncHttpClient的网络协议，转为HttpClient的网络协议类
                //parseProtocol(response.get),
                parseProtocol(HttpVersion.valueOf(protocol)),
                //响应码
                response.getStatusCode(),
                //消息
                response.getStatusText()
        );

        //把AsyncHttpClient的请求结果转换成HttpClient的请求结果
        BasicHttpResponse httpClientResponse = new BasicHttpResponse(responseStatus);
        //AsyncHttpClient响应转换为HttpClient的HttpEntity对象
        httpClientResponse.setEntity(entityFromAsyncHttpClientResponse(response));

        //响应头转换
        HttpHeaders responseHeaders = response.getHeaders();
        for (Map.Entry<String, String> header : responseHeaders) {
            String name = header.getKey();
            String value = header.getValue();
            httpClientResponse.addHeader(new BasicHeader(name, value));
        }
        return httpClientResponse;
    }

    /**
     * 获取协议
     */
    private String getProtocol(NettyResponse response) {
        try {
            Field statusField = response.getClass().getDeclaredField("status");
            statusField.setAccessible(true);
            HttpResponseStatus status = (HttpResponseStatus) statusField.get(response);
            return status.getProtocolText();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * AsyncHttpClient响应转换为HttpClient的HttpEntity对象
     */
    private static HttpEntity entityFromAsyncHttpClientResponse(Response response) throws IOException {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream responseBodyStream = response.getResponseBodyAsStream();
        //响应体信息
        entity.setContent(responseBodyStream);
        String contentLength = response.getHeader("Content-Length");
        entity.setContentLength(TextUtils.isEmpty(contentLength) ? 0 : Long.parseLong(contentLength));
        entity.setContentEncoding(response.getHeader("Content-Encoding"));
        //Content-Type
        if (response.getContentType() != null) {
            entity.setContentType(response.getContentType());
        }
        return entity;
    }

    private void setConnectionParametersForRequest(RequestBuilder builder, Request<?> request) throws AuthFailureError {
        //根据不同的请求方法，设置请求类型和请求体（POST、PUT、PATCH请求的参数放在请求体，而GET请求等是放在URL里面的，所以这里只设置有请求体的请求方法）
        switch (request.getMethod()) {
            case Request.Method.DEPRECATED_GET_OR_POST:
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    builder.setMethod(HttpConstants.Methods.GET);
                } else {
                    builder.setMethod(HttpConstants.Methods.POST);
                }
                setRequestBody(builder, request);
                break;
            case Request.Method.GET:
                builder.setMethod(HttpConstants.Methods.GET);
                break;
            case Request.Method.DELETE:
                builder.setMethod(HttpConstants.Methods.DELETE);
                break;
            case Request.Method.POST:
                builder.setMethod(HttpConstants.Methods.POST);
                setRequestBody(builder, request);
                break;
            case Request.Method.PUT:
                builder.setMethod(HttpConstants.Methods.PUT);
                setRequestBody(builder, request);
                break;
            case Request.Method.HEAD:
                builder.setMethod(HttpConstants.Methods.HEAD);
                break;
            case Request.Method.OPTIONS:
                builder.setMethod(HttpConstants.Methods.OPTIONS);
                break;
            case Request.Method.TRACE:
                builder.setMethod(HttpConstants.Methods.TRACE);
                break;
            case Request.Method.PATCH:
                builder.setMethod(HttpConstants.Methods.PATCH);
                setRequestBody(builder, request);
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    /**
     * 把AsyncHttpClient的网络协议，转为HttpClient的网络协议类
     */
    private static ProtocolVersion parseProtocol(final HttpVersion protocol) {
        if (protocol.equals(HttpVersion.HTTP_1_0)) {
            return new ProtocolVersion("HTTP", 1, 0);
        } else if (protocol.equals(HttpVersion.HTTP_1_1)) {
            return new ProtocolVersion("HTTP", 1, 1);
        } else if (protocol.protocolName().equalsIgnoreCase(ApplicationProtocolNames.SPDY_3)) {
            return new ProtocolVersion("SPDY", 3, 1);
        } else if (protocol.protocolName().equalsIgnoreCase(ApplicationProtocolNames.HTTP_2)) {
            return new ProtocolVersion("HTTP", 2, 0);
        }
        throw new IllegalAccessError("Unkwown protocol");
    }

    /**
     * 设置请求的body请求体
     */
    private static void setRequestBody(RequestBuilder builder, Request<?> request) throws AuthFailureError {
        byte[] postBody = request.getPostBody();
        builder.setBody(postBody);
        builder.setHeader("Content-Type", request.getPostBodyContentType());
    }

    private void shutdown() {
        if (mAsyncHttpClient != null) {
            try {
                mAsyncHttpClient.close();
                EventLoopGroup eventLoopGroup = mAsyncHttpClient.getConfig().getEventLoopGroup();
                if (eventLoopGroup != null) {
                    eventLoopGroup.shutdownGracefully();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }
}
