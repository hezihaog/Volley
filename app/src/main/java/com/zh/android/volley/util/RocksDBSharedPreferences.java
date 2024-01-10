package com.zh.android.volley.util;

import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;

import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于RocksDB实现的Key-Value存储
 */
public class RocksDBSharedPreferences implements SharedPreferences {
    /**
     * 监听器集合
     */
    private final List<OnSharedPreferenceChangeListener> mOnChangeListeners = new CopyOnWriteArrayList<>();

    private static class SingleHolder {
        private static final RocksDBSharedPreferences instance = new RocksDBSharedPreferences();
    }

    public static RocksDBSharedPreferences getInstance() {
        return RocksDBSharedPreferences.SingleHolder.instance;
    }

    /**
     * 获取数据库文件的存放目录
     *
     * @param databaseFileDirName 数据库文件目录名
     */
    private static String getDatabasePath(String databaseFileDirName) {
        return ContextUtil.getContext().getDatabasePath(databaseFileDirName).getAbsolutePath();
    }

    /**
     * 获取DB实例
     */
    private static RocksDB getRocksDB() {
        try {
            //获取数据库文件存放目录
            String dbPath = getDatabasePath("preferences");
            //创建存储数据库文件的文件夹
            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                dbFile.mkdirs();
            }
            return RocksDB.open(dbPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, ?> getAll() {
        Map<String, Object> resultMap = new HashMap<>();

        RocksDB db = getRocksDB();
        try {
            // 获取所有键值对的迭代器
            RocksIterator iterator = db.newIterator();

            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                byte[] key = iterator.key();
                byte[] value = iterator.value();
                resultMap.put(new String(key), new String(value));
            }

            // 关闭迭代器
            iterator.close();
        } finally {
            // 关闭数据库
            db.close();
        }

        return resultMap;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        String value = getByKey(key);
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        return value;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return null;
    }

    @Override
    public int getInt(String key, int defValue) {
        String value = getByKey(key);
        if (value == null || TextUtils.isEmpty(value)) {
            return defValue;
        }
        return Integer.parseInt(value);
    }

    @Override
    public long getLong(String key, long defValue) {
        String value = getByKey(key);
        if (value == null || TextUtils.isEmpty(value)) {
            return defValue;
        }
        return Long.parseLong(value);
    }

    @Override
    public float getFloat(String key, float defValue) {
        String value = getByKey(key);
        if (value == null || TextUtils.isEmpty(value)) {
            return defValue;
        }
        return Float.parseFloat(value);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        String value = getByKey(key);
        if (value == null || TextUtils.isEmpty(value)) {
            return defValue;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public boolean contains(String key) {
        return getByKey(key) != null;
    }

    @Override
    public Editor edit() {
        return new KVEditor(this);
    }

    private class KVEditor implements Editor {
        private final SharedPreferences mSharedPreferences;

        public KVEditor(SharedPreferences sharedPreferences) {
            mSharedPreferences = sharedPreferences;
        }

        @Override
        public Editor putString(String key, String value) {
            putByString(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> value) {
            putByString(key, JSON.toJSONString(value));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            putByString(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            putByString(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            putByString(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            putByString(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            RocksDB db = getRocksDB();
            try {
                db.delete(key.getBytes());
            } catch (RocksDBException e) {
                throw new RuntimeException(e);
            } finally {
                // 关闭数据库
                db.close();
            }
            return this;
        }

        @Override
        public Editor clear() {
            RocksDB db = getRocksDB();
            try {
                // 获取所有键值对的迭代器
                RocksIterator iterator = db.newIterator();
                iterator.seekToFirst();

                // 遍历,并删除所有键值对
                while (iterator.isValid()) {
                    byte[] key = iterator.key();
                    db.delete(key);
                    iterator.next();
                }

                // 关闭迭代器
                iterator.close();
            } catch (RocksDBException e) {
                throw new RuntimeException(e);
            } finally {
                // 关闭数据库
                db.close();
            }
            return this;
        }

        @Override
        public boolean commit() {
            return true;
        }

        @Override
        public void apply() {
        }

        /**
         * 存储统一用字符串
         */
        private void putByString(String key, Object value) {
            String valueStr = String.valueOf(value);

            RocksDB db = getRocksDB();
            try {
                db.put(key.getBytes(), valueStr.getBytes());
            } catch (RocksDBException e) {
                throw new RuntimeException(e);
            } finally {
                // 关闭数据库
                db.close();
            }

            for (OnSharedPreferenceChangeListener listener : mOnChangeListeners) {
                listener.onSharedPreferenceChanged(mSharedPreferences, key);
            }
        }
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (!mOnChangeListeners.contains(listener)) {
            mOnChangeListeners.add(listener);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mOnChangeListeners.remove(listener);
    }

    /**
     * 根据Key查询Value，因为存储都是String值，获取再进行转换
     *
     * @return 如果不存在，则返回null
     */
    private String getByKey(String key) {
        RocksDB db = getRocksDB();
        try {
            if (key == null) {
                return null;
            }
            byte[] valueByteArr = db.get(key.getBytes());
            if (valueByteArr == null) {
                valueByteArr = new byte[]{};
            }
            return valueByteArr.length > 0 ? new String(valueByteArr) : null;
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        } finally {
            // 关闭数据库
            db.close();
        }
    }
}