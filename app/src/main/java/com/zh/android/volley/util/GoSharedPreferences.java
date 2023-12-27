package com.zh.android.volley.util;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
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
        App.setDbPath(dbPath);
    }

    public static GoSharedPreferences getInstance() {
        return SingleHolder.instance;
    }

    /**
     * 获取数据库文件的存放目录
     */
    private static String getDatabasePath(String databaseFileName) {
        return ContextUtil.getContext().getDatabasePath(databaseFileName).getAbsolutePath();
    }

    @Override
    public Map<String, ?> getAll() {
        String allCacheDataJson = App.getAllCacheData();
        if (TextUtils.isEmpty(allCacheDataJson)) {
            return new HashMap<>();
        }
        //解析json为Map
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.<Map<String, Object>>fromJson(allCacheDataJson, type);
    }

    @Override
    public String getString(String key, String defValue) {
        String value = getByKey(key);
        if (value == null) {
            return defValue;
        }
        return value;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        throw new RuntimeException("不支持该功能");
    }

    @Override
    public int getInt(String key, int defValue) {
        String value = getByKey(key);
        if (value == null) {
            return defValue;
        }
        return Integer.parseInt(value);
    }

    @Override
    public long getLong(String key, long defValue) {
        String value = getByKey(key);
        if (value == null) {
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
        return Boolean.getBoolean(value);
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
        return App.getCacheData(key);
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
            putByString(key, value);
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
            App.deleteCacheData(key);
            return this;
        }

        @Override
        public Editor clear() {
            App.deleteAllCacheData();
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
            //重新插入
            App.setCacheData(key, String.valueOf(value));
            for (OnSharedPreferenceChangeListener listener : mOnChangeListeners) {
                listener.onSharedPreferenceChanged(mSharedPreferences, key);
            }
        }
    }
}