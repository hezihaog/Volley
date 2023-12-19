package com.zh.android.volley.volley.request;

import com.alibaba.fastjson.JSON;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

/**
 * FastJson自定义请求
 */
public class FastJsonRequest<T> extends Request<T> {

    private final Type mType;

    private final Response.Listener<T> mListener;

    public FastJsonRequest(String url, Type type, Response.Listener<T> listener, Response.ErrorListener errorlistener) {
        this(Method.GET, url, type, listener, errorlistener);
    }

    public FastJsonRequest(int method, String url, Type type, Response.Listener<T> listener, Response.ErrorListener errorlistener) {
        super(method, url, errorlistener);
        this.mType = type;
        this.mListener = listener;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            //将字符流转成字符串，并且设置字符编码
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            //使用FastJson将json转为对象，并且设置编码
            return Response.success(JSON.parseObject(jsonString, mType), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            //出错的时候，将错误信息重新调出
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }
}