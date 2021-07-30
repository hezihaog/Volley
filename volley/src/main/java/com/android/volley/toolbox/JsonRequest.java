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

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyLog;

import java.io.UnsupportedEncodingException;

/**
 * JSON请求
 */
public abstract class JsonRequest<T> extends Request<T> {
    /**
     * 默认字符集
     */
    protected static final String PROTOCOL_CHARSET = "utf-8";

    /**
     * JSON请求的Content-Type
     */
    private static final String PROTOCOL_CONTENT_TYPE =
            String.format("application/json; charset=%s", PROTOCOL_CHARSET);

    /**
     * 响应监听器
     */
    private Listener<T> mListener;
    /**
     * JSON体
     */
    private final String mRequestBody;

    /**
     * 废弃构造方法，默认GET请求，如果 {@link #getPostBody()} 或 {@link #getPostParams()} 被复写，则为POST请求
     */
    public JsonRequest(String url, String requestBody, Listener<T> listener,
                       ErrorListener errorListener) {
        this(Method.DEPRECATED_GET_OR_POST, url, requestBody, listener, errorListener);
    }

    /**
     * 手动指定请求方法，进行JSON请求
     */
    public JsonRequest(int method, String url, String requestBody, Listener<T> listener,
                       ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
        mRequestBody = requestBody;
    }

    @Override
    protected void onFinish() {
        super.onFinish();
        //请求结束，清理回调
        mListener = null;
    }

    @Override
    protected void deliverResponse(T response) {
        //回调响应成功
        if (mListener != null) {
            mListener.onResponse(response);
        }
    }

    /**
     * 子类进行JsonObject或JsonArray的处理
     *
     * @param response 响应实体
     */
    @Override
    abstract protected Response<T> parseNetworkResponse(NetworkResponse response);

    /**
     * @deprecated Use {@link #getBodyContentType()}.
     */
    @Override
    public String getPostBodyContentType() {
        return getBodyContentType();
    }

    /**
     * @deprecated Use {@link #getBody()}.
     */
    @Override
    public byte[] getPostBody() {
        return getBody();
    }

    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    @Override
    public byte[] getBody() {
        //JSON字符串转换为byte数组
        try {
            return mRequestBody == null ? null : mRequestBody.getBytes(PROTOCOL_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                    mRequestBody, PROTOCOL_CHARSET);
            return null;
        }
    }
}
