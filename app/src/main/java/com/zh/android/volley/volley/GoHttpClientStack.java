package com.zh.android.volley.volley;

import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpStack;
import com.zh.android.volley.util.GoHttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.ProtocolVersion;
import cz.msebera.android.httpclient.entity.BasicHttpEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.message.BasicHttpResponse;
import cz.msebera.android.httpclient.message.BasicStatusLine;

/**
 * Go的HttpClient实现的Volley网络层
 */
public class GoHttpClientStack implements HttpStack {
    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        //请求超时时间
        int timeoutMs = request.getTimeoutMs();
        //请求方式
        String method = mapMethodStr(request);
        //请求参数
        byte[] postBody = request.getPostBody();
        String bodyParams;
        if (postBody != null && postBody.length > 0) {
            bodyParams = new String(postBody);
        } else {
            bodyParams = "";
        }
        //请求头
        String headersStr = getHeadersStr(request, additionalHeaders);

        //发送请求，并获取响应
        GoHttpClient.GoClientResponse response = GoHttpClient.sendRequest(
                method,
                request.getUrl(),
                bodyParams,
                headersStr,
                timeoutMs
        );

        //存在错误
        if (!TextUtils.isEmpty(response.getError())) {
            throw new IOException(response.getError());
        }

        //转换响应状态行
        BasicStatusLine responseStatus = new BasicStatusLine(
                //把Go返回的网络协议，转为HttpClient的网络协议类
                parseProtocol(response.getProtocolVersion()),
                //响应码
                response.getStatusCode(),
                //消息
                response.getRespLine()
        );

        //把GoHttpClient的请求结果转换成HttpClient的请求结果
        BasicHttpResponse httpClientResponse = new BasicHttpResponse(responseStatus);
        //GoHttpClient响应转换为HttpClient的HttpEntity对象
        httpClientResponse.setEntity(entityFromGoHttpClientResponse(response));

        //响应头转换
        Map<String, ArrayList<String>> responseHeaders = response.getRespHeaders();
        for (Map.Entry<String, ArrayList<String>> header : responseHeaders.entrySet()) {
            String name = header.getKey();
            ArrayList<String> values = header.getValue();
            String value = values == null || values.isEmpty() ? "" : values.get(0);
            httpClientResponse.addHeader(new BasicHeader(name, value));
        }
        return httpClientResponse;
    }

    /**
     * 把Go的网络协议，转为HttpClient的网络协议类
     */
    private static ProtocolVersion parseProtocol(final String protocol) {
        if ("HTTP/1.1".equals(protocol)) {
            return new ProtocolVersion("HTTP", 1, 0);
        } else if ("HTTP/1.0".equals(protocol)) {
            return new ProtocolVersion("HTTP", 1, 1);
        } else if ("SPDY/3".equals(protocol)) {
            return new ProtocolVersion("SPDY", 3, 1);
        } else if ("h2".equals(protocol)) {
            return new ProtocolVersion("HTTP", 2, 0);
        }
        throw new IllegalAccessError("Unkwown protocol");
    }

    /**
     * Go响应转换为HttpClient的HttpEntity对象
     */
    private static HttpEntity entityFromGoHttpClientResponse(GoHttpClient.GoClientResponse response) throws IOException {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream responseBodyStream = strToInputStream(response.getBodyString());

        //响应体信息
        entity.setContent(responseBodyStream);
        //响应头
        Map<String, ArrayList<String>> respHeaders = response.getRespHeaders();

        String contentLengthStr = getResponseHeader(respHeaders, "Content-Length");
        long contentLength = TextUtils.isEmpty(contentLengthStr) ? 0 : Long.parseLong(contentLengthStr);
        entity.setContentLength(contentLength);

        String contentEncoding = getResponseHeader(respHeaders, "Content-Encoding");
        entity.setContentEncoding(contentEncoding);

        //Content-Type
        String contentType = getResponseHeader(respHeaders, "Content-Type");
        if (contentType != null) {
            entity.setContentType(contentType);
        }
        return entity;
    }

    /**
     * 获取响应头
     */
    private static String getResponseHeader(Map<String, ArrayList<String>> respHeaders, String key) {
        ArrayList<String> values = respHeaders.get(key);
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.get(0);
    }

    /**
     * 字符串转InputStream
     */
    private static InputStream strToInputStream(String str) {
        byte[] byteArray = str.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(byteArray);
    }

    /**
     * 获取请求方式
     */
    private String mapMethodStr(Request<?> request) throws AuthFailureError {
        switch (request.getMethod()) {
            case Request.Method.DEPRECATED_GET_OR_POST:
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    return "POST";
                } else {
                    return "GET";
                }
            case Request.Method.GET:
                return "GET";
            case Request.Method.DELETE:
                return "DELETE";
            case Request.Method.POST:
                return "POST";
            case Request.Method.PUT:
                return "PUT";
            case Request.Method.HEAD:
                return "HEAD";
            case Request.Method.OPTIONS:
                return "OPTIONS";
            case Request.Method.TRACE:
                return "TRACE";
            case Request.Method.PATCH:
                return "PATCH";
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    /**
     * 将请求头转为字符串，格式：Content-Type=application/x-www-form-urlencoded&token=123
     */
    private String getHeadersStr(Request<?> request, Map<String, String> additionalHeaders) throws AuthFailureError {
        HashMap<String, String> resultHeaders = new HashMap<>();

        Map<String, String> headers = request.getHeaders();
        resultHeaders.putAll(headers);
        resultHeaders.putAll(additionalHeaders);

        //设置ContentType
        resultHeaders.put("Content-Type", request.getPostBodyContentType());

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : resultHeaders.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }
        return builder.toString();
    }
}
