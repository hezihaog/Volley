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

import java.io.UnsupportedEncodingException;

/**
 * 响应为String类型的Request
 */
public class StringRequest extends Request<String> {
    private Listener<String> mListener;

    /**
     * 创建一个StringRequest，可以指定请求方法
     *
     * @param method        请求方法 {@link Method}
     * @param url           URL
     * @param listener      响应监听器
     * @param errorListener 错误监听器
     */
    public StringRequest(int method, String url, Listener<String> listener,
                         ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
    }

    /**
     * 创建一个StringRequest，使用GET请求
     *
     * @param url           URL
     * @param listener      响应监听器
     * @param errorListener 错误监听器
     */
    public StringRequest(String url, Listener<String> listener, ErrorListener errorListener) {
        this(Method.GET, url, listener, errorListener);
    }

    @Override
    protected void onFinish() {
        super.onFinish();
        //请求完成时，清理回调
        mListener = null;
    }

    @Override
    protected void deliverResponse(String response) {
        //回调响应
        if (mListener != null) {
            mListener.onResponse(response);
        }
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        //解析响应
        String parsed;
        try {
            //指定字符串级生成字符串
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            //生成失败，直接用原始数据
            parsed = new String(response.data);
        }
        //返回响应
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }
}
