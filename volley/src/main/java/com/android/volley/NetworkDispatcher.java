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

import android.annotation.TargetApi;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;

import java.util.concurrent.BlockingQueue;

/**
 * 网络请求分发器
 */
public class NetworkDispatcher extends Thread {
    /**
     * 网络请求队列
     */
    private final BlockingQueue<Request<?>> mQueue;
    /**
     * 网络请求发起操作类
     */
    private final Network mNetwork;
    /**
     * 缓存类
     */
    private final Cache mCache;
    /**
     * 请求响应结果分发器
     */
    private final ResponseDelivery mDelivery;
    /**
     * 是否退出
     */
    private volatile boolean mQuit = false;

    /**
     * 创建一个网络请求分发器，必须要调用 {@link #start()} 才能开始工作
     *
     * @param queue    网络请求队列
     * @param network  网络请求发起操作类
     * @param cache    缓存类
     * @param delivery 请求响应结果分发器
     */
    public NetworkDispatcher(BlockingQueue<Request<?>> queue,
                             Network network, Cache cache,
                             ResponseDelivery delivery) {
        mQueue = queue;
        mNetwork = network;
        mCache = cache;
        mDelivery = delivery;
    }

    /**
     * 退出
     */
    public void quit() {
        //设置退出标志
        mQuit = true;
        //中断线程
        interrupt();
    }

    /**
     * 流量统计
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void addTrafficStatsTag(Request<?> request) {
        // Tag the request (if API >= 14)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            TrafficStats.setThreadStatsTag(request.getTrafficStatsTag());
        }
    }

    @Override
    public void run() {
        //设置线程优先级
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Request<?> request;
        //死循环，不断从网络请求队列中获取请求
        while (true) {
            long startTimeMs = SystemClock.elapsedRealtime();
            //释放之前的请求对象，避免泄露
            request = null;
            try {
                //从网络请求队列中获取请求
                request = mQueue.take();
            } catch (InterruptedException e) {
                //退出了，停止
                if (mQuit) {
                    return;
                }
                continue;
            }
            try {
                request.addMarker("network-queue-take");
                //请求被取消，结束该请求
                if (request.isCanceled()) {
                    request.finish("network-discard-cancelled");
                    continue;
                }

                //流量统计
                addTrafficStatsTag(request);

                //执行网络请求
                NetworkResponse networkResponse = mNetwork.performRequest(request);
                request.addMarker("network-http-complete");

                //服务端返回了304，并且我们已经响应到主线程了，就算完成请求了，结束该请求
                if (networkResponse.notModified && request.hasHadResponseDelivered()) {
                    request.finish("not-modified");
                    continue;
                }

                //在子线程中，解析该请求
                Response<?> response = request.parseNetworkResponse(networkResponse);
                request.addMarker("network-parse-complete");

                // 如果该请求需要被缓存，则缓存该请求的结果
                if (request.shouldCache() && response.cacheEntry != null) {
                    mCache.put(request.getCacheKey(), response.cacheEntry);
                    request.addMarker("network-cache-written");
                }

                request.markDelivered();
                //把解析好的数据，在主线程中回调
                mDelivery.postResponse(request, response);
            } catch (VolleyError volleyError) {
                volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
                parseAndDeliverNetworkError(request, volleyError);
            } catch (Exception e) {
                VolleyLog.e(e, "Unhandled exception %s", e.toString());
                VolleyError volleyError = new VolleyError(e);
                volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
                mDelivery.postError(request, volleyError);
            }
        }
    }

    /**
     * 解析并且回调网络错误
     */
    private void parseAndDeliverNetworkError(Request<?> request, VolleyError error) {
        error = request.parseNetworkError(error);
        mDelivery.postError(request, error);
    }
}
