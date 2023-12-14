package com.zh.android.volley.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpStack;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp实现的Volley网络层
 */
public class OkHttpStack implements HttpStack {
    private OkHttpClient mOkHttpClient;

    /**
     * 无参构造，没有传入OkHttpClient，直接创建默认的OkHttpClient实例
     */
    public OkHttpStack() {
        this(createDefaultOkHttpClient());
    }

    /**
     * 外部传入OkHttpClient，则使用它来构建请求
     */
    public OkHttpStack(OkHttpClient okHttpClient) {
        if (okHttpClient == null) {
            throw new IllegalArgumentException("OkHttpClient can't be null");
        }
        mOkHttpClient = okHttpClient;
    }

    /**
     * 创建默认的OkHttpClient
     */
    private static OkHttpClient createDefaultOkHttpClient() {
        int defaultTimeoutMs = DefaultRetryPolicy.DEFAULT_TIMEOUT_MS;
        return new OkHttpClient.Builder()
                .connectTimeout(defaultTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(defaultTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(defaultTimeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        int timeoutMs = request.getTimeoutMs();
        OkHttpClient client;
        //3个超时时间都不一样时，重新构建一个OkHttpClient，才可以设置
        if (timeoutMs != mOkHttpClient.connectTimeoutMillis() &&
                timeoutMs != mOkHttpClient.readTimeoutMillis() &&
                timeoutMs != mOkHttpClient.writeTimeoutMillis()) {
            client = mOkHttpClient.newBuilder()
                    .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .build();
        } else {
            client = mOkHttpClient;
        }
        //下次以最新请求的配置为准
        mOkHttpClient = client;

        //创建OkHttp的请求
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        //设置请求Url
        builder.url(request.getUrl());

        //添加请求Header
        Map<String, String> headers = request.getHeaders();
        for (String name : headers.keySet()) {
            builder.addHeader(name, headers.get(name));
        }
        for (String name : additionalHeaders.keySet()) {
            builder.addHeader(name, additionalHeaders.get(name));
        }

        //设置请求的参数
        setConnectionParametersForRequest(builder, request);

        //构建请求
        okhttp3.Request okRequest = builder.build();
        Call call = client.newCall(okRequest);
        //发起同步请求，获取相应
        Response okResponse = call.execute();

        //转换相应状态行
        BasicStatusLine responseStatus = new BasicStatusLine(
                //把OkHttp的网络协议，转为HttpClient的网络协议类
                parseProtocol(okResponse.protocol()),
                //响应码
                okResponse.code(),
                //消息
                okResponse.message()
        );

        //把OkHttp的请求结果转换成HttpClient的请求结果
        BasicHttpResponse httpClientResponse = new BasicHttpResponse(responseStatus);
        //OkHttp响应转换为HttpClient的HttpEntity对象
        httpClientResponse.setEntity(entityFromOkHttpResponse(okResponse));

        //响应头转换
        Headers responseHeaders = okResponse.headers();
        int size = responseHeaders.size();
        for (int i = 0; i < size; i++) {
            String name = responseHeaders.name(i);
            String value = responseHeaders.value(i);
            httpClientResponse.addHeader(new BasicHeader(name, value));
        }
        return httpClientResponse;
    }

    /**
     * OkHttp响应转换为HttpClient的HttpEntity对象
     */
    private static HttpEntity entityFromOkHttpResponse(Response response) throws IOException {
        BasicHttpEntity entity = new BasicHttpEntity();
        ResponseBody body = response.body();
        //响应体信息
        entity.setContent(body.byteStream());
        entity.setContentLength(body.contentLength());
        entity.setContentEncoding(response.header("Content-Encoding"));
        //Content-Type
        if (body.contentType() != null) {
            entity.setContentType(body.contentType().type());
        }
        return entity;
    }

    /**
     * 设置请求的参数
     */
    static void setConnectionParametersForRequest(okhttp3.Request.Builder builder, Request<?> request) throws IOException, AuthFailureError {
        //根据不同的请求方法，设置请求类型和请求体（POST、PUT、PATCH请求的参数放在请求体，而GET请求等是放在URL里面的，所以这里只设置有请求体的请求方法）
        switch (request.getMethod()) {
            case Request.Method.DEPRECATED_GET_OR_POST:
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    builder.post(RequestBody.create(MediaType.parse(request.getPostBodyContentType()), postBody));
                }
                break;
            case Request.Method.GET:
                builder.get();
                break;
            case Request.Method.DELETE:
                builder.delete();
                break;
            case Request.Method.POST:
                builder.post(createRequestBody(request));
                break;
            case Request.Method.PUT:
                builder.put(createRequestBody(request));
                break;
            case Request.Method.HEAD:
                builder.head();
                break;
            case Request.Method.OPTIONS:
                builder.method("OPTIONS", null);
                break;
            case Request.Method.TRACE:
                builder.method("TRACE", null);
                break;
            case Request.Method.PATCH:
                builder.patch(createRequestBody(request));
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    /**
     * 把OkHttp的网络协议，转为HttpClient的网络协议类
     */
    private static ProtocolVersion parseProtocol(final Protocol protocol) {
        switch (protocol) {
            case HTTP_1_0:
                return new ProtocolVersion("HTTP", 1, 0);
            case HTTP_1_1:
                return new ProtocolVersion("HTTP", 1, 1);
            case SPDY_3:
                return new ProtocolVersion("SPDY", 3, 1);
            case HTTP_2:
                return new ProtocolVersion("HTTP", 2, 0);
        }
        throw new IllegalAccessError("Unkwown protocol");
    }

    /**
     * 通过OkHttp的请求，创建RequestBody
     */
    private static RequestBody createRequestBody(Request<?> request) throws AuthFailureError {
        final byte[] body = request.getBody();
        if (body == null) {
            throw new NullPointerException(request.getMethod() + "请求的请求体不能为空");
        }
        return RequestBody.create(MediaType.parse(request.getBodyContentType()), body);
    }
}