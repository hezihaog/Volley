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

/**
 * 响应实体
 */
public class Response<T> {
    /**
     * 响应结果回调接口
     */
    public interface Listener<T> {
        /**
         * 收到响应时回调
         */
        void onResponse(T response);
    }

    /**
     * 错误回调接口
     */
    public interface ErrorListener {
        /**
         * 收到错误时回调
         */
        void onErrorResponse(VolleyError error);
    }

    /**
     * 构建一个成功的Response实例
     */
    public static <T> Response<T> success(T result, Cache.Entry cacheEntry) {
        return new Response<T>(result, cacheEntry);
    }

    /**
     * 构建一个失败的Response实例
     */
    public static <T> Response<T> error(VolleyError error) {
        return new Response<T>(error);
    }

    /**
     * 解析到的响应结果，请求错误时为null
     */
    public final T result;

    /**
     * 缓存信息，请求错误时为null
     */
    public final Cache.Entry cacheEntry;

    /**
     * 错误异常
     */
    public final VolleyError error;

    /**
     * 为true，则代表这个为软过期响应
     */
    public boolean intermediate = false;

    /**
     * 判断该响应是否成功
     */
    public boolean isSuccess() {
        return error == null;
    }

    private Response(T result, Cache.Entry cacheEntry) {
        this.result = result;
        this.cacheEntry = cacheEntry;
        this.error = null;
    }

    private Response(VolleyError error) {
        this.result = null;
        this.cacheEntry = null;
        this.error = error;
    }
}