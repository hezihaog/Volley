package com.zh.android.volley.volley.request;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;


/**
 * Gson自定义请求
 */
public class JacksonRequest<T> extends Request<T> {
    private static final ObjectMapper sObjectMapper = new ObjectMapper()
            //忽略不识别的字段
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            //忽略不识别的枚举
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

    private final Type mType;

    private final Listener<T> mListener;

    public JacksonRequest(String url, Type type, Listener<T> listener, ErrorListener errorlistener) {
        this(Method.GET, url, type, listener, errorlistener);
    }

    public JacksonRequest(int method, String url, Type type, Listener<T> listener, ErrorListener errorlistener) {
        super(method, url, errorlistener);
        this.mType = type;
        this.mListener = listener;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            //将字符流转成字符串，并且设置字符编码
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            //使用Jackson将json转为对象，并且设置编码
            return Response.success(sObjectMapper.readValue(jsonString, new TypeReference<T>() {
                @Override
                public Type getType() {
                    return mType;
                }
            }), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            //出错的时候，将错误信息重新调出
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }
}