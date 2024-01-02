package com.zh.android.volley;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;

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
        startFileHttpServer();
        startWebSocketServer();
    }

    private void startFileHttpServer() {
        //获取数据库文件存放目录
        String dbPath = getDatabasePath("file").getAbsolutePath();
        //创建存储数据库文件的文件夹
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            dbFile.mkdirs();
        }
        //配置文件上传历史记录数据库
        App.configFileUploadHistoryDir(dbFile.getAbsolutePath());
        //配置文件上传的文件存储目录
        App.configFileUploadDir(getApplication().getExternalCacheDir().getAbsolutePath());
        //启动文件服务
        App.startFileHttpServer(8080);
    }

    private void startWebSocketServer() {
        App.startWebSocketServer(9001, "/ws", "");
    }
}