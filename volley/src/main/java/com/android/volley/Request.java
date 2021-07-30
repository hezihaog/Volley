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

package com.android.volley;

import android.net.TrafficStats;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.android.volley.VolleyLog.MarkerLog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

/**
 * Base class for all network requests.
 *
 * @param <T> The type of parsed response this request expects.
 */
public abstract class Request<T> implements Comparable<Request<T>> {

    /**
     * Default encoding for POST or PUT parameters. See {@link #getParamsEncoding()}.
     */
    private static final String DEFAULT_PARAMS_ENCODING = "UTF-8";

    /**
     * 支持的请求方法
     */
    public interface Method {
        int DEPRECATED_GET_OR_POST = -1;
        int GET = 0;
        int POST = 1;
        int PUT = 2;
        int DELETE = 3;
        int HEAD = 4;
        int OPTIONS = 5;
        int TRACE = 6;
        int PATCH = 7;
    }

    /**
     * An event log tracing the lifetime of this request; for debugging.
     */
    private final MarkerLog mEventLog = MarkerLog.ENABLED ? new MarkerLog() : null;

    /**
     * 当前请求的请求方法，支持 GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, and PATCH.
     */
    private final int mMethod;

    /**
     * 请求的请求地址Url
     */
    private final String mUrl;

    /**
     * 出现 3xx http响应码的重定向url
     */
    private String mRedirectUrl;

    /**
     * 请求的唯一标识
     */
    private final String mIdentifier;

    /**
     * Default tag for {@link TrafficStats}.
     */
    private final int mDefaultTrafficStatsTag;

    /**
     * 错误的回调监听器
     */
    private Response.ErrorListener mErrorListener;

    /**
     * 请求的序列号，用于请求排序，先进先出
     */
    private Integer mSequence;

    /**
     * 请求绑定请求队列
     */
    private RequestQueue mRequestQueue;

    /**
     * 是否需要缓存，默认开启
     */
    private boolean mShouldCache = true;

    /**
     * 请求是否被取消的标志位
     */
    private boolean mCanceled = false;

    /**
     * 请求是否已被响应
     */
    private boolean mResponseDelivered = false;

    /**
     * 重试策略
     */
    private RetryPolicy mRetryPolicy;

    /**
     * 缓存信息对象
     */
    private Cache.Entry mCacheEntry = null;

    /**
     * 请求的Tag，取消请求时使用
     */
    private Object mTag;

    /**
     * Creates a new request with the given URL and error listener.  Note that
     * the normal response listener is not provided here as delivery of responses
     * is provided by subclasses, who have a better idea of how to deliver an
     * already-parsed response.
     *
     * @deprecated Use {@link #Request(int, String, Response.ErrorListener)}.
     */
    @Deprecated
    public Request(String url, Response.ErrorListener listener) {
        this(Method.DEPRECATED_GET_OR_POST, url, listener);
    }

    /**
     * 构造方法，创建一个Request对象
     *
     * @param method   请求方法
     * @param url      请求Url
     * @param listener 错误回调监听器
     */
    public Request(int method, String url, Response.ErrorListener listener) {
        mMethod = method;
        mUrl = url;
        //创建请求的唯一标识
        mIdentifier = createIdentifier(method, url);
        mErrorListener = listener;
        //设置默认的重试策略
        setRetryPolicy(new DefaultRetryPolicy());

        mDefaultTrafficStatsTag = findDefaultTrafficStatsTag(url);
    }

    /**
     * Return the method for this request.  Can be one of the values in {@link Method}.
     */
    public int getMethod() {
        return mMethod;
    }

    /**
     * Set a tag on this request. Can be used to cancel all requests with this
     * tag by {@link RequestQueue#cancelAll(Object)}.
     *
     * @return This Request object to allow for chaining.
     */
    public Request<?> setTag(Object tag) {
        mTag = tag;
        return this;
    }

    /**
     * Returns this request's tag.
     *
     * @see Request#setTag(Object)
     */
    public Object getTag() {
        return mTag;
    }

    /**
     * @return this request's {@link Response.ErrorListener}.
     */
    public Response.ErrorListener getErrorListener() {
        return mErrorListener;
    }

    /**
     * @return A tag for use with {@link TrafficStats#setThreadStatsTag(int)}
     */
    public int getTrafficStatsTag() {
        return mDefaultTrafficStatsTag;
    }

    /**
     * 返回Url的host（主机地址）的hashcode，没有的话，返回0
     */
    private static int findDefaultTrafficStatsTag(String url) {
        if (!TextUtils.isEmpty(url)) {
            Uri uri = Uri.parse(url);
            if (uri != null) {
                String host = uri.getHost();
                if (host != null) {
                    return host.hashCode();
                }
            }
        }
        return 0;
    }

    /**
     * Sets the retry policy for this request.
     *
     * @return This Request object to allow for chaining.
     */
    public Request<?> setRetryPolicy(RetryPolicy retryPolicy) {
        mRetryPolicy = retryPolicy;
        return this;
    }

    /**
     * Adds an event to this request's event log; for debugging.
     */
    public void addMarker(String tag) {
        if (MarkerLog.ENABLED) {
            mEventLog.add(tag, Thread.currentThread().getId());
        }
    }

    /**
     * 通知RequestQueue队列，该请求已经结束
     */
    void finish(final String tag) {
        if (mRequestQueue != null) {
            //通知队列，移除掉该请求
            mRequestQueue.finish(this);
            onFinish();
        }
        //打印日志
        if (MarkerLog.ENABLED) {
            final long threadId = Thread.currentThread().getId();
            //判断当前线程如果是不是主线程
            if (Looper.myLooper() != Looper.getMainLooper()) {
                //不是主线程，通过Handler，保证Log打印是有序的
                Handler mainThread = new Handler(Looper.getMainLooper());
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mEventLog.add(tag, threadId);
                        mEventLog.finish(this.toString());
                    }
                });
                return;
            }
            mEventLog.add(tag, threadId);
            mEventLog.finish(this.toString());
        }
    }

    /**
     * 结束时，清除监听器
     */
    protected void onFinish() {
        mErrorListener = null;
    }

    /**
     * Associates this request with the given queue. The request queue will be notified when this
     * request has finished.
     *
     * @return This Request object to allow for chaining.
     */
    public Request<?> setRequestQueue(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
        return this;
    }

    /**
     * Sets the sequence number of this request.  Used by {@link RequestQueue}.
     *
     * @return This Request object to allow for chaining.
     */
    public final Request<?> setSequence(int sequence) {
        mSequence = sequence;
        return this;
    }

    /**
     * Returns the sequence number of this request.
     */
    public final int getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        }
        return mSequence;
    }

    /**
     * Returns the URL of this request.
     */
    public String getUrl() {
        return (mRedirectUrl != null) ? mRedirectUrl : mUrl;
    }

    /**
     * Returns the URL of the request before any redirects have occurred.
     */
    public String getOriginUrl() {
        return mUrl;
    }

    /**
     * Returns the identifier of the request.
     */
    public String getIdentifier() {
        return mIdentifier;
    }

    /**
     * Sets the redirect url to handle 3xx http responses.
     */
    public void setRedirectUrl(String redirectUrl) {
        mRedirectUrl = redirectUrl;
    }

    /**
     * 返回这个请求的缓存Key，默认由请求方法和请求Url组合
     */
    public String getCacheKey() {
        return mMethod + ":" + mUrl;
    }

    /**
     * 设置缓存信息对象
     */
    public Request<?> setCacheEntry(Cache.Entry entry) {
        mCacheEntry = entry;
        return this;
    }

    /**
     * 返回缓存信息对象，如果没有则为null
     */
    public Cache.Entry getCacheEntry() {
        return mCacheEntry;
    }

    /**
     * 标记该请求已被取消，如果被取消，则不会进行回调
     */
    public void cancel() {
        mCanceled = true;
    }

    /**
     * 返回该请求是否被取消了
     */
    public boolean isCanceled() {
        return mCanceled;
    }

    /**
     * 返回配置的请求头
     *
     * @throws AuthFailureError In the event of auth failure
     */
    public Map<String, String> getHeaders() throws AuthFailureError {
        return Collections.emptyMap();
    }

    /**
     * Returns a Map of POST parameters to be used for this request, or null if
     * a simple GET should be used.  Can throw {@link AuthFailureError} as
     * authentication may be required to provide these values.
     *
     * <p>Note that only one of getPostParams() and getPostBody() can return a non-null
     * value.</p>
     *
     * @throws AuthFailureError In the event of auth failure
     * @deprecated Use {@link #getParams()} instead.
     */
    @Deprecated
    protected Map<String, String> getPostParams() throws AuthFailureError {
        return getParams();
    }

    /**
     * Returns which encoding should be used when converting POST parameters returned by
     * {@link #getPostParams()} into a raw POST body.
     *
     * <p>This controls both encodings:
     * <ol>
     *     <li>The string encoding used when converting parameter names and values into bytes prior
     *         to URL encoding them.</li>
     *     <li>The string encoding used when converting the URL encoded parameters into a raw
     *         byte array.</li>
     * </ol>
     *
     * @deprecated Use {@link #getParamsEncoding()} instead.
     */
    @Deprecated
    protected String getPostParamsEncoding() {
        return getParamsEncoding();
    }

    /**
     * @deprecated Use {@link #getBodyContentType()} instead.
     */
    @Deprecated
    public String getPostBodyContentType() {
        return getBodyContentType();
    }

    /**
     * 返回Body
     *
     * @throws AuthFailureError In the event of auth failure
     * @deprecated Use {@link #getBody()} instead.
     */
    @Deprecated
    public byte[] getPostBody() throws AuthFailureError {
        // Note: For compatibility with legacy clients of volley, this implementation must remain
        // here instead of simply calling the getBody() function because this function must
        // call getPostParams() and getPostParamsEncoding() since legacy clients would have
        // overridden these two member functions for POST requests.
        Map<String, String> postParams = getPostParams();
        if (postParams != null && postParams.size() > 0) {
            return encodeParameters(postParams, getPostParamsEncoding());
        }
        return null;
    }

    /**
     * 返回POST和PUT时的请求参数，子类可复写该方法设置自定义参数
     */
    protected Map<String, String> getParams() throws AuthFailureError {
        return null;
    }

    /**
     * 返回在POST和PUT时，参数转换使用的编码
     *
     * <p>This controls both encodings:
     * <ol>
     *     <li>The string encoding used when converting parameter names and values into bytes prior
     *         to URL encoding them.</li>
     *     <li>The string encoding used when converting the URL encoded parameters into a raw
     *         byte array.</li>
     * </ol>
     */
    protected String getParamsEncoding() {
        return DEFAULT_PARAMS_ENCODING;
    }

    /**
     * 返回请求的Content-Type
     */
    public String getBodyContentType() {
        return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
    }

    /**
     * 返回请求体，请求方式为：POST和PUT时
     *
     * <p>By default, the body consists of the request parameters in
     * application/x-www-form-urlencoded format. When overriding this method, consider overriding
     * {@link #getBodyContentType()} as well to match the new body format.
     *
     * @throws AuthFailureError in the event of auth failure
     */
    public byte[] getBody() throws AuthFailureError {
        Map<String, String> params = getParams();
        if (params != null && params.size() > 0) {
            return encodeParameters(params, getParamsEncoding());
        }
        return null;
    }

    /**
     * 将请求参数Map，转换为指定编码的字符串，例如：https://www.baidu.com/?a=xxx&b=yyy
     * <p>
     * Converts <code>params</code> into an application/x-www-form-urlencoded encoded string.
     *
     * @param params         参数Map
     * @param paramsEncoding 编码
     */
    private byte[] encodeParameters(Map<String, String> params, String paramsEncoding) {
        StringBuilder encodedParams = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
                encodedParams.append('=');
                encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
                encodedParams.append('&');
            }
            return encodedParams.toString().getBytes(paramsEncoding);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
        }
    }

    /**
     * 设置该请求是否可以被缓存
     *
     * @param shouldCache 是否可以被缓存
     */
    public final Request<?> setShouldCache(boolean shouldCache) {
        mShouldCache = shouldCache;
        return this;
    }

    /**
     * 该请求是否应该被缓存
     */
    public final boolean shouldCache() {
        return mShouldCache;
    }

    /**
     * 优先级，请求顺序会按照优先级进行处理，高优先级会优先于低优先级执行，默认按照先进先出的规则
     */
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }

    /**
     * 返回请求的优先级 {@link Priority}，默认优先级为 {@link Priority#NORMAL}
     */
    public Priority getPriority() {
        return Priority.NORMAL;
    }

    /**
     * 获取请求的超时时间
     */
    public final int getTimeoutMs() {
        return mRetryPolicy.getCurrentTimeout();
    }

    /**
     * 返回该请求的重试策略
     */
    public RetryPolicy getRetryPolicy() {
        return mRetryPolicy;
    }

    /**
     * 标记该请求已被响应过了
     */
    public void markDelivered() {
        mResponseDelivered = true;
    }

    /**
     * 返回该请求是否被响应过了
     */
    public boolean hasHadResponseDelivered() {
        return mResponseDelivered;
    }

    /**
     * 抽象方法，子类进行实现，这个方法在子线程中调用，
     *
     * @param response 网络响应
     * @return 解析后的响应，如果出错，则返回null
     */
    abstract protected Response<T> parseNetworkResponse(NetworkResponse response);

    /**
     * 解析请求错误，子类可以复写该方法，返回更加具体的错误类型，默认类型是VolleyError
     */
    protected VolleyError parseNetworkError(VolleyError volleyError) {
        return volleyError;
    }

    /**
     * 抽象方法，子类必须实现，该方法用于通知监听器获取响应结果
     *
     * @param response 响应
     */
    abstract protected void deliverResponse(T response);

    /**
     * 请求失败时，回调错误监听器
     *
     * @param error 错误信息
     */
    public void deliverError(VolleyError error) {
        if (mErrorListener != null) {
            mErrorListener.onErrorResponse(error);
        }
    }

    /**
     * 比较方法
     * <p>
     * 优先按照优先级进行排序，其次再按照序列号进行先进先出排序
     */
    @Override
    public int compareTo(Request<T> other) {
        //获取要比较的2个请求的请求优先级
        Priority left = this.getPriority();
        Priority right = other.getPriority();

        //默认优先级为NORMAL
        //如果优先级相等，则比较请求加入的顺序排序，先进先出
        return left == right ?
                this.mSequence - other.mSequence :
                right.ordinal() - left.ordinal();
    }

    @Override
    public String toString() {
        String trafficStatsTag = "0x" + Integer.toHexString(getTrafficStatsTag());
        return (mCanceled ? "[X] " : "[ ] ") + getUrl() + " " + trafficStatsTag + " "
                + getPriority() + " " + mSequence;
    }

    /**
     * 唯一标识的生成次数
     */
    private static long sCounter;

    /**
     * 创建请求的唯一标识，它是通过请求方法和请求Url组合成一个字符串后，进行sha1Hash算法计算得出
     * <p>
     * sha1(Request:method:url:timestamp:counter)
     *
     * @param method 请求方法
     * @param url    请求Url
     * @return sha1 hash string
     */
    private static String createIdentifier(final int method, final String url) {
        return InternalUtils.sha1Hash("Request:" + method + ":" + url +
                ":" + System.currentTimeMillis() + ":" + (sCounter++));
    }
}
