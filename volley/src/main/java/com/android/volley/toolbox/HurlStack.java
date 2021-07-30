/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley.toolbox;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Request.Method;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * 基于HttpURLConnection实现的请求实现类
 */
public class HurlStack implements HttpStack {
    /**
     * Content-Type请求头标记
     */
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * URL转换接口
     */
    public interface UrlRewriter {
        /**
         * 在请求前，可以重写URL
         *
         * @param originalUrl 原始URL
         * @return 返回重写后的URL，返回null则代表这个请求不应该被请求，会抛出异常
         */
        String rewriteUrl(String originalUrl);
    }

    private final UrlRewriter mUrlRewriter;
    private final SSLSocketFactory mSslSocketFactory;

    public HurlStack() {
        this(null);
    }

    /**
     * @param urlRewriter 重写URL
     */
    public HurlStack(UrlRewriter urlRewriter) {
        this(urlRewriter, null);
    }

    /**
     * @param urlRewriter      重写URL
     * @param sslSocketFactory 用于HTTPS连接的SSL工厂
     */
    public HurlStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
        mUrlRewriter = urlRewriter;
        mSslSocketFactory = sslSocketFactory;
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {
        //获取请求Url
        String url = request.getUrl();
        //获取请求头
        HashMap<String, String> map = new HashMap<String, String>();
        map.putAll(request.getHeaders());
        map.putAll(additionalHeaders);
        //请求前，可以重写URL
        if (mUrlRewriter != null) {
            String rewritten = mUrlRewriter.rewriteUrl(url);
            if (rewritten == null) {
                throw new IOException("URL blocked by rewriter: " + url);
            }
            url = rewritten;
        }
        //解析URL
        URL parsedUrl = new URL(url);
        //打开URL连接
        HttpURLConnection connection = openConnection(parsedUrl, request);
        //设置请求头
        for (String headerName : map.keySet()) {
            connection.addRequestProperty(headerName, map.get(headerName));
        }
        //设置请求参数和请求方法
        setConnectionParametersForRequest(connection, request);
        // Initialize HttpResponse with data from the HttpURLConnection.
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        //获取响应码
        int responseCode = connection.getResponseCode();
        if (responseCode == -1) {
            //响应状态码无法获取会返回-1，要抛出异常给调用方
            throw new IOException("Could not retrieve response code from HttpUrlConnection.");
        }
        //设置响应码和响应信息到响应对象中
        StatusLine responseStatus = new BasicStatusLine(protocolVersion,
                connection.getResponseCode(), connection.getResponseMessage());
        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        //有响应体则把响应体转换为HttpEntity设置到响应中
        if (hasResponseBody(request.getMethod(), responseStatus.getStatusCode())) {
            response.setEntity(entityFromConnection(connection));
        }
        //设置头
        for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                Header h = new BasicHeader(header.getKey(), header.getValue().get(0));
                response.addHeader(h);
            }
        }
        //返回响应信息
        return response;
    }

    /**
     * 检查响应是否有响应体
     *
     * @param requestMethod 请求方法
     * @param responseCode  响应状态码
     */
    private static boolean hasResponseBody(int requestMethod, int responseCode) {
        return requestMethod != Method.HEAD
                && !(HttpStatus.SC_CONTINUE <= responseCode && responseCode < HttpStatus.SC_OK)
                && responseCode != HttpStatus.SC_NO_CONTENT
                && responseCode != HttpStatus.SC_NOT_MODIFIED;
    }

    /**
     * 把指定的HttpURLConnection转换为HttpEntity
     */
    private static HttpEntity entityFromConnection(HttpURLConnection connection) {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        entity.setContent(inputStream);
        entity.setContentLength(connection.getContentLength());
        entity.setContentEncoding(connection.getContentEncoding());
        entity.setContentType(connection.getContentType());
        return entity;
    }

    /**
     * 以指定的URL，创建一个HttpURLConnection连接
     */
    protected HttpURLConnection createConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * 打开一个带参数的HttpURLConnection
     *
     * @param url URL地址
     * @return 链接
     * @throws IOException 异常
     */
    private HttpURLConnection openConnection(URL url, Request<?> request) throws IOException {
        HttpURLConnection connection = createConnection(url);

        int timeoutMs = request.getTimeoutMs();
        //设置超时时间
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        //如果是https链接，设置调用时配置的SSL工厂
        if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(mSslSocketFactory);
        }

        return connection;
    }

    /**
     * 设置请求参数和请求方法
     */
    @SuppressWarnings("deprecation")
    /* package */ static void setConnectionParametersForRequest(HttpURLConnection connection,
                                                                Request<?> request) throws IOException, AuthFailureError {
        //设置请求方法
        switch (request.getMethod()) {
            case Method.DEPRECATED_GET_OR_POST:
                //一种不建议使用的方式，如果Body不为空，则设置为POST请求
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    // Prepare output. There is no need to set Content-Length explicitly,
                    // since this is handled by HttpURLConnection using the size of the prepared
                    // output stream.
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    //设置Content-Type
                    connection.addRequestProperty(HEADER_CONTENT_TYPE,
                            request.getPostBodyContentType());
                    //设置请求体
                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.write(postBody);
                    out.close();
                }
                break;
            case Method.GET:
                //默认不设置请求头的话，HttpURLConnection的请求方法为GET，这里再显式设置一下
                connection.setRequestMethod("GET");
                break;
            case Method.DELETE:
                connection.setRequestMethod("DELETE");
                break;
            case Method.POST:
                connection.setRequestMethod("POST");
                addBodyIfExists(connection, request);
                break;
            case Method.PUT:
                connection.setRequestMethod("PUT");
                addBodyIfExists(connection, request);
                break;
            case Method.HEAD:
                connection.setRequestMethod("HEAD");
                break;
            case Method.OPTIONS:
                connection.setRequestMethod("OPTIONS");
                break;
            case Method.TRACE:
                connection.setRequestMethod("TRACE");
                break;
            case Method.PATCH:
                connection.setRequestMethod("PATCH");
                addBodyIfExists(connection, request);
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    /***
     * 设置请求体，如果有的话
     */
    private static void addBodyIfExists(HttpURLConnection connection, Request<?> request)
            throws IOException, AuthFailureError {
        byte[] body = request.getBody();
        if (body != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getBodyContentType());
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.write(body);
            out.close();
        }
    }
}
