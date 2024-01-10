package com.zh.android.volley.util;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import app.App;

/**
 * 基于Go实现的Key-Value存储
 */
public class GoSharedPreferences implements SharedPreferences {
    /**
     * 监听器集合
     */
    private final CopyOnWriteArrayList<OnSharedPreferenceChangeListener> mOnChangeListeners = new CopyOnWriteArrayList<>();

    private static class SingleHolder {
        private static final GoSharedPreferences instance = new GoSharedPreferences();
    }

    static {
        //获取数据库文件存放目录
        String dbPath = getDatabasePath("preferences");
        //创建存储数据库文件的文件夹
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            dbFile.mkdirs();
        }
        App.configDiskCacheDir(dbPath);
    }

    public static GoSharedPreferences getInstance() {
        return SingleHolder.instance;
    }

    /**
     * 获取数据库文件的存放目录
     *
     * @param databaseFileDirName 数据库文件目录名
     */
    private static String getDatabasePath(String databaseFileDirName) {
        return ContextUtil.getContext().getDatabasePath(databaseFileDirName).getAbsolutePath();
    }

    @Override
    public Map<String, ?> getAll() {
        //如果内存缓存中有，则使用内存缓存
        String allMemoryCacheJson = App.getAllMemoryCache();
        if (!TextUtils.isEmpty(allMemoryCacheJson)) {
            return parseAllData(allMemoryCacheJson);
        }
        //没有则使用磁盘缓存
        String allDiskCacheJson = App.getAllDiskCache();
        if (TextUtils.isEmpty(allDiskCacheJson)) {
            return new HashMap<>();
        }
        return parseAllData(allDiskCacheJson);
    }

    private Map<String, ?> parseAllData(String allCacheDataJson) {
        //解析json为Map
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.<Map<String, Object>>fromJson(allCacheDataJson, type);
    }

    @Override
    public String getString(String key, String defValue) {
        String value = getByKey(key);
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        return value;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        String value = getByKey(key);
        if (TextUtils.isEmpty(value)) {
            return defValues;
        }
        return new HashSet<>(JSONArray.parseArray(value, String.class));
    }

    @Override
    public int getInt(String key, int defValue) {
        String value = getByKey(key);
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        return Integer.parseInt(value);
    }

    @Override
    public long getLong(String key, long defValue) {
        String value = getByKey(key);
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        return Long.parseLong(value);
    }

    @Override
    public float getFloat(String key, float defValue) {
        String value = getByKey(key);
        if (value == null) {
            return defValue;
        }
        return Long.parseLong(value);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        String value = getByKey(key);
        if (value == null) {
            return defValue;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public boolean contains(String key) {
        return getByKey(key) != null;
    }

    /**
     * 根据Key查询Value，因为存储都是String值，获取再进行转换
     *
     * @return 如果不存在，则返回null
     */
    private String getByKey(String key) {
        if (key == null) {
            return null;
        }
        String value = App.getMemoryCache(key);
        return !TextUtils.isEmpty(value) ? value : App.getDiskCache(key);
    }

    @Override
    public Editor edit() {
        return new KVEditor(this);
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
            App.deleteMemoryCache(key);
            App.deleteDiskCache(key);
            return this;
        }

        @Override
        public Editor clear() {
            App.clearMemoryCache();
            App.clearDiskCache();
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

            //内存缓存
            App.setMemoryCache(key, valueStr, -1);
            //磁盘缓存
            App.setDiskCache(key, valueStr);

            for (OnSharedPreferenceChangeListener listener : mOnChangeListeners) {
                listener.onSharedPreferenceChanged(mSharedPreferences, key);
            }
        }
    }
}