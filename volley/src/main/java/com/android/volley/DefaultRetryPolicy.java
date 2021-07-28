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
 * 默认重试策略
 */
public class DefaultRetryPolicy implements RetryPolicy {
    /**
     * 超时时间
     */
    private int mCurrentTimeoutMs;

    /**
     * 已重试的次数
     */
    private int mCurrentRetryCount;

    /**
     * 最大重试次数
     */
    private final int mMaxNumRetries;

    /**
     * 失败后，重连的间隔因子
     */
    private final float mBackoffMultiplier;

    /**
     * 默认超时时间
     */
    public static final int DEFAULT_TIMEOUT_MS = 2500;

    /**
     * 默认重试次数
     */
    public static final int DEFAULT_MAX_RETRIES = 0;

    /**
     * 默认失败之后重连的间隔因子为1
     */
    public static final float DEFAULT_BACKOFF_MULT = 1f;


    /**
     * 使用默认参数，进行构造
     */
    public DefaultRetryPolicy() {
        this(DEFAULT_TIMEOUT_MS, DEFAULT_MAX_RETRIES, DEFAULT_BACKOFF_MULT);
    }

    /**
     * 构造方法
     *
     * @param initialTimeoutMs  超时时间
     * @param maxNumRetries     最大重试次数
     * @param backoffMultiplier 重试间隔
     */
    public DefaultRetryPolicy(int initialTimeoutMs, int maxNumRetries, float backoffMultiplier) {
        mCurrentTimeoutMs = initialTimeoutMs;
        mMaxNumRetries = maxNumRetries;
        mBackoffMultiplier = backoffMultiplier;
    }

    /**
     * 获取超时时间
     */
    @Override
    public int getCurrentTimeout() {
        return mCurrentTimeoutMs;
    }

    /**
     * 获取，已重试的次数
     */
    @Override
    public int getCurrentRetryCount() {
        return mCurrentRetryCount;
    }

    /**
     * 获取，失败后，重连的间隔因子
     */
    public float getBackoffMultiplier() {
        return mBackoffMultiplier;
    }

    /**
     * 重试
     */
    @Override
    public void retry(VolleyError error) throws VolleyError {
        //重试1次，记录数量
        mCurrentRetryCount++;
        //计算重试时间
        mCurrentTimeoutMs += (mCurrentTimeoutMs * mBackoffMultiplier);
        if (!hasAttemptRemaining()) {
            //到达重试最大次数，还是失败了，抛异常
            throw error;
        }
    }

    /**
     * 判断是否还可以重试，还可以重试返回true，不可以重试返回false
     */
    protected boolean hasAttemptRemaining() {
        return mCurrentRetryCount <= mMaxNumRetries;
    }
}
