package com.zh.android.volley.util;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import app.App;
import app.HttpRequestCallback;

/**
 * Go的Http请求客户端
 */
public class GoHttpClient {
    /**
     * 主线程Handler
     */
    private static final Handler mMainHandler = new Handler(Looper.getMainLooper());

    /**
     * 同步发送Http请求
     *
     * @param method     请求方式
     * @param url        请求路径
     * @param bodyParams body字符串
     * @param headersStr 请求头
     * @param timeoutMs  超时时间
     * @return 响应实体
     */
    public static GoClientResponse sendRequest(String method, String url, String bodyParams, String headersStr, int timeoutMs) {
        //发送请求，并获取响应Json，格式是自定义的
        String responseJson = App.sendRequest(
                method,
                url,
                bodyParams,
                headersStr,
                timeoutMs
        );
        //解析响应Json为实体类
        return GoHttpClient.parseJson2GoClientResponse(responseJson);
    }

    public interface RequestCallback {
        void onSuccess(GoClientResponse response);

        void onFail(String errorMsg);
    }

    /**
     * 异步发送Http请求
     */
    public static void sendRequestAsync(String method, String url, String bodyParams, String headersStr, int timeoutMs, RequestCallback callback) {
        //发送请求，并获取响应Json，格式是自定义的
        App.sendRequestAsync(
                method,
                url,
                bodyParams,
                headersStr,
                timeoutMs,
                new HttpRequestCallback() {
                    @Override
                    public void onResponse(String responseJson) {
                        //解析响应Json为实体类
                        GoClientResponse response = GoHttpClient.parseJson2GoClientResponse(responseJson);
                        mMainHandler.post(() -> {
                            if (!TextUtils.isEmpty(response.getError())) {
                                if (callback != null) {
                                    callback.onFail(response.getError());
                                }
                            } else {
                                if (callback != null) {
                                    callback.onSuccess(response);
                                }
                            }
                        });
                    }
                }
        );
    }

    /**
     * 解析Json为GoClientResponse
     */
    private static GoHttpClient.GoClientResponse parseJson2GoClientResponse(String responseJson) {
        Map<String, Object> jsonMap = jsonToMap(responseJson);
        // 状态码
        String statusCode = String.valueOf(jsonMap.get("statusCode"));
        // 响应体
        String bodyString = String.valueOf(jsonMap.get("bodyString"));
        // 响应行
        String respLine = String.valueOf(jsonMap.get("respLine"));
        // 协议版本
        String protocolVersion = String.valueOf(jsonMap.get("protocolVersion"));
        // 响应头
        Map<String, ArrayList<String>> respHeaders = (Map<String, ArrayList<String>>) jsonMap.get("respHeaders");
        // 错误信息
        Object error = jsonMap.get("error");
        String errorStr = error != null ? String.valueOf(error) : "";

        boolean invalidStatusCode = TextUtils.isEmpty(statusCode) || "null".equalsIgnoreCase(statusCode);

        return new GoHttpClient.GoClientResponse(
                Integer.parseInt(invalidStatusCode ? "0" : statusCode),
                bodyString,
                respLine,
                protocolVersion,
                respHeaders,
                errorStr
        );
    }

    /**
     * Json字符串转Map
     */
    private static Map<String, Object> jsonToMap(String json) {
        Map<String, Object> map = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);
                if (value instanceof JSONObject) {
                    value = jsonToMap(value.toString());
                } else if (value instanceof JSONArray) {
                    value = jsonArrayToList((JSONArray) value);
                }
                map.put(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private static List<Object> jsonArrayToList(JSONArray jsonArray) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                value = jsonToMap(value.toString());
            } else if (value instanceof JSONArray) {
                value = jsonArrayToList((JSONArray) value);
            }
            list.add(value);
        }
        return list;
    }

    public static class GoClientResponse {
        /**
         * 响应状态码
         */
        int statusCode;

        /**
         * 响应体
         */
        String bodyString;

        /**
         * 响应行
         */
        String respLine;

        /**
         * 协议版本
         */
        String protocolVersion;

        /**
         * 响应头
         */
        Map<String, ArrayList<String>> respHeaders;

        /**
         * 错误信息
         */
        String error;

        public GoClientResponse(int statusCode, String bodyString, String respLine, String protocolVersion, Map<String, ArrayList<String>> respHeaders, String error) {
            this.statusCode = statusCode;
            this.bodyString = bodyString;
            this.respLine = respLine;
            this.protocolVersion = protocolVersion;
            this.respHeaders = respHeaders;
            this.error = error;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBodyString() {
            return bodyString;
        }

        public String getRespLine() {
            return respLine;
        }

        public String getProtocolVersion() {
            return protocolVersion;
        }

        public Map<String, ArrayList<String>> getRespHeaders() {
            return respHeaders;
        }

        public String getError() {
            return error;
        }
    }
}