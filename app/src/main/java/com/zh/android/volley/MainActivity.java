package com.zh.android.volley;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

import app.App;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindView();
        setData();
    }

    private void bindView() {
        //标题栏
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        //HttpUrlConnection实现
        findViewById(R.id.get_request_http_url_connection_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(MainActivity.this, HomeActivity.TYPE_HTTP_URL_CONNECTION);
            }
        });
        //HttpClient实现
        findViewById(R.id.get_request_http_client_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(MainActivity.this, HomeActivity.TYPE_HTTP_CLIENT);
            }
        });
        //OkHttp实现
        findViewById(R.id.get_request_okhttp_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(MainActivity.this, HomeActivity.TYPE_OKHTTP);
            }
        });
        //AsyncHttpClient实现
        findViewById(R.id.get_request_async_http_client_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(MainActivity.this, HomeActivity.TYPE_ASYNC_HTTP_CLIENT);
            }
        });
        //GoHttpClient实现
        findViewById(R.id.get_request_go_http_client_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(MainActivity.this, HomeActivity.TYPE_GO_HTTP_CLIENT);
            }
        });
    }

    private void setData() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "Wally");
        map.put("age", "18");
        map.put("sex", "男");
        App.startHttpServer(8080, "/", JSONObject.toJSONString(map));
    }
}