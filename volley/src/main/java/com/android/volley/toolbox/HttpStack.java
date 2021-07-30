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

import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Http协议栈接口
 */
public interface HttpStack {
    /**
     * 使用指定参数，执行Http请求
     *
     * request.getPostBody() == null，则发送GET请求，不为null则发送POST请求
     * 并且，Content-Type请求头的取值从 request.getPostBodyContentType() 中获取
     *
     * @param request           请求对象
     * @param additionalHeaders 附带的请求头
     * @return HTTP的响应
     */
    HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError;
}