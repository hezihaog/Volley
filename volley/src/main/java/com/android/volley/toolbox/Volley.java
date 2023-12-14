/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import com.android.volley.Network;
import com.android.volley.RequestQueue;

import java.io.File;

import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

public class Volley {
    /**
     * 默认磁盘缓存目录名
     */
    private static final String DEFAULT_CACHE_DIR = "volley";

    /**
     * 创建请求队列，支持设置请求实现和最大磁盘缓存大小
     *
     * @param stack             请求实现，传null则自动根据版本选择
     * @param maxDiskCacheBytes 最大磁盘缓存大小，传-1则使用默认的5Ms
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack, int maxDiskCacheBytes) {
        //缓存目录
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);

        //设置UA
        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (NameNotFoundException e) {
        }

        //没有指定请求实现，根据版本选择
        if (stack == null) {
            if (Build.VERSION.SDK_INT >= 9) {
                stack = new HurlStack();
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                stack = new HttpClientStack(new DefaultHttpClient());
            }
        }

        //创建网络操作实现
        Network network = new BasicNetwork(stack);

        RequestQueue queue;
        //根据传入的磁盘缓存大小，创建请求队列
        if (maxDiskCacheBytes <= -1) {
            //没有设置，使用默认值创建
            queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
        } else {
            //设置了，则使用指定的值
            queue = new RequestQueue(new DiskBasedCache(cacheDir, maxDiskCacheBytes), network);
        }

        //开启队列
        queue.start();
        return queue;
    }

    /**
     * 创建请求队列，支持设置请求实现和最大磁盘缓存大小
     *
     * @param maxDiskCacheBytes 最大磁盘缓存大小
     */
    public static RequestQueue newRequestQueue(Context context, int maxDiskCacheBytes) {
        return newRequestQueue(context, null, maxDiskCacheBytes);
    }

    /**
     * 创建请求队列，支持设置请求实现
     *
     * @param stack 请求实现
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack) {
        return newRequestQueue(context, stack, -1);
    }

    /**
     * 创建请求队列
     */
    public static RequestQueue newRequestQueue(Context context) {
        return newRequestQueue(context, null);
    }
}