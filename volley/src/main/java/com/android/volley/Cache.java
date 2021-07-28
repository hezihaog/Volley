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

import java.util.Collections;
import java.util.Map;

/**
 * 一个缓存接口，Key为String
 */
public interface Cache {
    /**
     * 查询缓存
     *
     * @param key 缓存Ke
     * @return An {@link Entry} or null in the event of a cache miss
     */
    Entry get(String key);

    /**
     * 添加或更新缓存
     *
     * @param key   缓存Key
     * @param entry Data to store and metadata for cache coherency, TTL, etc.
     */
    void put(String key, Entry entry);

    /**
     * 缓存初始化，该方法在子线程中回调
     */
    void initialize();

    /**
     * 让一个缓存失效
     *
     * @param key        缓存Key
     * @param fullExpire true为完全过期，false为软过期
     */
    void invalidate(String key, boolean fullExpire);

    /**
     * 移除一个缓存
     *
     * @param key 缓存Key
     */
    void remove(String key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 缓存实体，保存请求数据
     */
    class Entry {
        /**
         * 缓存的数据
         */
        public byte[] data;

        /**
         * ETag 保证缓存一致性
         */
        public String etag;

        /**
         * 服务端返回数据的时间
         */
        public long serverDate;

        /**
         * 缓存对象的最后修改日期
         */
        public long lastModified;

        /**
         * 记录TTL
         */
        public long ttl;

        /**
         * 记录 Soft TTL
         */
        public long softTtl;

        /**
         * 服务端返回的不可变的响应头
         */
        public Map<String, String> responseHeaders = Collections.emptyMap();

        /**
         * 返回true，则代表过期
         */
        public boolean isExpired() {
            return this.ttl < System.currentTimeMillis();
        }

        /**
         * 是否需要刷新数据
         */
        public boolean refreshNeeded() {
            return this.softTtl < System.currentTimeMillis();
        }
    }
}