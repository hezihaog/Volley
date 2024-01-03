package com.zh.android.volley;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.zh.android.volley.util.AssetUtils;
import com.zh.android.volley.util.ToastUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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
        findViewById(R.id.close_all_server_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<Integer> ports = Arrays.asList(8181, 8080, 9001);
                for (Integer port : ports) {
                    App.stopServer(port);
                }
                ToastUtil.toast(getApplicationContext(), "关闭所有服务成功");
            }
        });
    }

    private void setData() {
        startHttpServer();
        startFileHttpServer();
        startWebSocketServer();
    }

    private void startHttpServer() {
        //网站的根目录文件夹名称
        String websiteDirName = "web";
        //拷贝到的外部文件夹路径
        String outDirPath = new File(
                getApplicationContext().getExternalCacheDir().getAbsolutePath(),
                websiteDirName
        ).getAbsolutePath();
        //开始拷贝
        AssetUtils.copyFilesFromAssets(
                getApplicationContext(),
                websiteDirName,
                outDirPath
        );
        App.startHttpServer(8181, "/", outDirPath, outDirPath + "/static", "index.html");
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