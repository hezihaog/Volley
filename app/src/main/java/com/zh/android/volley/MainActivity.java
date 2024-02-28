package com.zh.android.volley;

import android.view.View;

import androidx.appcompat.widget.Toolbar;

import com.zh.android.volley.base.BaseActivity;

public class MainActivity extends BaseActivity {
    @Override
    public int onInflaterViewId() {
        return R.layout.activity_main;
    }

    @Override
    public void onBindView(View view) {
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
        findViewById(R.id.get_request_okhttp_btn).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                HomeActivity.start(MainActivity.this, HomeActivity.TYPE_OKHTTP_WITH_CRONET);
                return true;
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
        //curl实现
        findViewById(R.id.get_request_curl_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(MainActivity.this, HomeActivity.TYPE_CURL);
            }
        });
    }
}