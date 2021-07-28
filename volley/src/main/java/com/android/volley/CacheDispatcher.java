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

import android.os.Process;

import java.util.concurrent.BlockingQueue;

/**
 * 缓存分发器
 */
public class CacheDispatcher extends Thread {
    /**
     * Debug模式标记
     */
    private static final boolean DEBUG = VolleyLog.DEBUG;

    /**
     * 缓存队列，是一个BlockingQueue
     */
    private final BlockingQueue<Request<?>> mCacheQueue;

    /**
     * 网络队列，也是一个BlockingQueue
     */
    private final BlockingQueue<Request<?>> mNetworkQueue;

    /**
     * 缓存类
     */
    private final Cache mCache;

    /**
     * 网络请求结果分发器
     */
    private final ResponseDelivery mDelivery;

    /**
     * 是否退出的标记
     */
    private volatile boolean mQuit = false;

    /**
     * 构造方法，创建一个缓存分发器，必须调用 {@link #start()} 才能开始处理
     *
     * @param cacheQueue   缓存队列
     * @param networkQueue 网络队列
     * @param cache        缓存类
     * @param delivery     请求结果分发器
     */
    public CacheDispatcher(
            BlockingQueue<Request<?>> cacheQueue, BlockingQueue<Request<?>> networkQueue,
            Cache cache, ResponseDelivery delivery) {
        mCacheQueue = cacheQueue;
        mNetworkQueue = networkQueue;
        mCache = cache;
        mDelivery = delivery;
    }

    /**
     * 停止缓存分发器
     */
    public void quit() {
        //设置标记
        mQuit = true;
        //中断线程
        interrupt();
    }

    @Override
    public void run() {
        if (DEBUG) VolleyLog.v("start new dispatcher");
        //设置线程优先级
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        //缓存初始化
        mCache.initialize();

        Request<?> request;
        //死循环，不断从队列中取出请求
        while (true) {
            //每次执行前，清除之前的请求对象，避免泄露
            request = null;
            try {
                //从缓存队列中获取一个请求
                request = mCacheQueue.take();
            } catch (InterruptedException e) {
                //被打断了，则退出
                if (mQuit) {
                    return;
                }
                continue;
            }
            try {
                request.addMarker("cache-queue-take");

                //请求被取消，结束请求即可
                if (request.isCanceled()) {
                    request.finish("cache-discard-canceled");
                    continue;
                }

                //尝试从缓存中查找缓存信息
                Cache.Entry entry = mCache.get(request.getCacheKey());
                if (entry == null) {
                    request.addMarker("cache-miss");
                    //没有找到缓存，加入到网络请求队列
                    mNetworkQueue.put(request);
                    continue;
                }

                //缓存过期，也加入网络请求队列
                if (entry.isExpired()) {
                    request.addMarker("cache-hit-expired");
                    request.setCacheEntry(entry);
                    mNetworkQueue.put(request);
                    continue;
                }

                //命中缓存，解析它
                request.addMarker("cache-hit");
                Response<?> response = request.parseNetworkResponse(
                        new NetworkResponse(entry.data, entry.responseHeaders));
                request.addMarker("cache-hit-parsed");

                if (!entry.refreshNeeded()) {
                    //完全没有过期，在主线程回调结果
                    mDelivery.postResponse(request, response);
                } else {
                    //软过期，在主线程回调结果，但也要请求网络
                    request.addMarker("cache-hit-refresh-needed");
                    request.setCacheEntry(entry);

                    //设置为软过期标记
                    response.intermediate = true;

                    //在主线程中回调
                    final Request<?> finalRequest = request;
                    mDelivery.postResponse(request, response, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //把请求，添加到请求队列中
                                mNetworkQueue.put(finalRequest);
                            } catch (InterruptedException e) {
                                //添加失败，不处理了
                            }
                        }
                    });
                }
            } catch (Exception e) {
                VolleyLog.e(e, "Unhandled exception %s", e.toString());
            }
        }
    }
}
