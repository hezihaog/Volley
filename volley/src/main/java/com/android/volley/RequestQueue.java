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

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求队列管理类
 */
public class RequestQueue {
    /**
     * 请求完成监听器
     */
    public interface RequestFinishedListener<T> {
        /**
         * 请求完成时，回调
         */
        void onRequestFinished(Request<T> request);
    }

    /**
     * 用于生成请求序列号
     */
    private final AtomicInteger mSequenceGenerator = new AtomicInteger();

    /**
     * Staging area for requests that already have a duplicate request in flight.
     *
     * <ul>
     *     <li>containsKey(cacheKey) indicates that there is a request in flight for the given cache
     *          key.</li>
     *     <li>get(cacheKey) returns waiting requests for the given cache key. The in flight request
     *          is <em>not</em> contained in that list. Is null if no requests are staged.</li>
     * </ul>
     */
    private final Map<String, Queue<Request<?>>> mWaitingRequests = new HashMap<>();

    /**
     * 正在请求中的请求集合
     */
    private final Set<Request<?>> mCurrentRequests = new HashSet<>();

    /**
     * 请求的缓存队列，是一个PriorityBlockingQueue，可以根据优先级来出队
     */
    private final PriorityBlockingQueue<Request<?>> mCacheQueue = new PriorityBlockingQueue<>();

    /**
     * 请求的网络队列，也是一个PriorityBlockingQueue
     */
    private final PriorityBlockingQueue<Request<?>> mNetworkQueue = new PriorityBlockingQueue<>();

    /**
     * 请求分发器的数量，默认为4个
     */
    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 4;

    /**
     * 缓存类，用于保存缓存和查询缓存
     */
    private final Cache mCache;

    /**
     * 网络请求执行器
     */
    private final Network mNetwork;

    /**
     * 网络请求结果分发器
     */
    private final ResponseDelivery mDelivery;

    /**
     * 网络请求分发器数组
     */
    private final NetworkDispatcher[] mDispatchers;

    /**
     * 缓存分发器
     */
    private CacheDispatcher mCacheDispatcher;

    /**
     * 网络请求完成后的监听器集合
     */
    private final List<RequestFinishedListener> mFinishedListeners = new ArrayList<>();

    /**
     * 创建一个请求队列RequestQueue，要调用 {@link #start()} ，队列才会开始工作
     *
     * @param cache          用于把缓存到磁盘
     * @param network        用于执行请求
     * @param threadPoolSize 用于设置网络请求分发器的线程池数量
     * @param delivery       用于回调请求结果的分发器
     */
    public RequestQueue(Cache cache, Network network, int threadPoolSize,
                        ResponseDelivery delivery) {
        mCache = cache;
        mNetwork = network;
        mDispatchers = new NetworkDispatcher[threadPoolSize];
        mDelivery = delivery;
    }

    /**
     * 创建一个请求队列RequestQueue，可以指定分发器的个数
     */
    public RequestQueue(Cache cache, Network network, int threadPoolSize) {
        this(cache, network, threadPoolSize,
                new ExecutorDelivery(new Handler(Looper.getMainLooper())));
    }

    /**
     * 创建一个请求队列RequestQueue，使用默认配置
     */
    public RequestQueue(Cache cache, Network network) {
        this(cache, network, DEFAULT_NETWORK_THREAD_POOL_SIZE);
    }

    /**
     * 让队列开始工作
     */
    public void start() {
        //停止正在进行的分发器，包括缓存分发器和网络分发器
        stop();
        //创建缓存分发器
        mCacheDispatcher = new CacheDispatcher(mCacheQueue, mNetworkQueue, mCache, mDelivery);
        //启动缓存分发器
        mCacheDispatcher.start();
        //根据定义的分发器数组数量，创建指定数量的网络分发器
        for (int i = 0; i < mDispatchers.length; i++) {
            NetworkDispatcher networkDispatcher = new NetworkDispatcher(mNetworkQueue, mNetwork,
                    mCache, mDelivery);
            mDispatchers[i] = networkDispatcher;
            //启动缓存分发器
            networkDispatcher.start();
        }
    }

    /**
     * 停止缓存分发器和网络分发器
     */
    public void stop() {
        if (mCacheDispatcher != null) {
            mCacheDispatcher.quit();
        }
        for (int i = 0; i < mDispatchers.length; i++) {
            if (mDispatchers[i] != null) {
                mDispatchers[i].quit();
            }
        }
    }

    /**
     * Gets a sequence number.
     */
    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }

    /**
     * Gets the {@link Cache} instance being used.
     */
    public Cache getCache() {
        return mCache;
    }

    /**
     * 过滤器接口
     */
    public interface RequestFilter {
        //判断是否过滤
        boolean apply(Request<?> request);
    }

    /**
     * Cancels all requests in this queue for which the given filter applies.
     *
     * @param filter The filtering function to use
     */
    public void cancelAll(RequestFilter filter) {
        //同步锁
        synchronized (mCurrentRequests) {
            //遍历当前请求集合
            for (Request<?> request : mCurrentRequests) {
                //匹配到了，则取消请求
                if (filter.apply(request)) {
                    request.cancel();
                }
            }
        }
    }

    /**
     * 取消指定tag标记的请求
     */
    public void cancelAll(final Object tag) {
        //Tag不可以为空
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancelAll with a null tag");
        }
        cancelAll(new RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                //匹配到了
                return request.getTag() == tag;
            }
        });
    }

    /**
     * 添加一个请求到分发队列中
     */
    public <T> Request<T> add(Request<T> request) {
        //请求保存该队列，用于标识该请求属于该队列
        request.setRequestQueue(this);
        //同步添加请求对正在进行的请求集合中
        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }

        //给请求设置序列号
        request.setSequence(getSequenceNumber());
        //设置Marker标记为
        request.addMarker("add-to-queue");

        //如果请求不需要缓存，那添加到网络队列中就可以了
        if (!request.shouldCache()) {
            mNetworkQueue.add(request);
            return request;
        }

        // Insert request into stage if there's already a request with the same cache key in flight.
        synchronized (mWaitingRequests) {
            String cacheKey = request.getCacheKey();
            if (mWaitingRequests.containsKey(cacheKey)) {
                // There is already a request in flight. Queue up.
                Queue<Request<?>> stagedRequests = mWaitingRequests.get(cacheKey);
                if (stagedRequests == null) {
                    stagedRequests = new LinkedList<Request<?>>();
                }
                stagedRequests.add(request);
                mWaitingRequests.put(cacheKey, stagedRequests);
                if (VolleyLog.DEBUG) {
                    VolleyLog.v("Request for cacheKey=%s is in flight, putting on hold.", cacheKey);
                }
            } else {
                // Insert 'null' queue for this cacheKey, indicating there is now a request in
                // flight.
                mWaitingRequests.put(cacheKey, null);
                mCacheQueue.add(request);
            }
            return request;
        }
    }

    /**
     * 结束请求
     */
    <T> void finish(Request<T> request) {
        //从正在进行中的请求集合中移除该请求
        synchronized (mCurrentRequests) {
            mCurrentRequests.remove(request);
        }
        //通知回调，请求结束了
        synchronized (mFinishedListeners) {
            for (RequestFinishedListener<T> listener : mFinishedListeners) {
                listener.onRequestFinished(request);
            }
        }
        //如果该请求可以被缓存
        if (request.shouldCache()) {
            synchronized (mWaitingRequests) {
                String cacheKey = request.getCacheKey();
                Queue<Request<?>> waitingRequests = mWaitingRequests.remove(cacheKey);
                if (waitingRequests != null) {
                    if (VolleyLog.DEBUG) {
                        VolleyLog.v("Releasing %d waiting requests for cacheKey=%s.",
                                waitingRequests.size(), cacheKey);
                    }
                    // Process all queued up requests. They won't be considered as in flight, but
                    // that's not a problem as the cache has been primed by 'request'.
                    mCacheQueue.addAll(waitingRequests);
                }
            }
        }
    }

    /**
     * 添加一个请求结束的回调
     */
    public <T> void addRequestFinishedListener(RequestFinishedListener<T> listener) {
        synchronized (mFinishedListeners) {
            mFinishedListeners.add(listener);
        }
    }

    /**
     * 移除设置的请求结束回调
     */
    public <T> void removeRequestFinishedListener(RequestFinishedListener<T> listener) {
        synchronized (mFinishedListeners) {
            mFinishedListeners.remove(listener);
        }
    }
}
