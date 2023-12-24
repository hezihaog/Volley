package com.zh.android.volley;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.zh.android.volley.util.ToastUtil;

/**
 * WebView页面
 */
public class WebActivity extends AppCompatActivity {
    private Toolbar vToolbar;
    private WebView vWebView;

    public static void start(Activity activity, String link) {
        Intent intent = new Intent(activity, WebActivity.class);
        intent.putExtra(Constant.KEY_WEB_LINK, link);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        findView();
        bindView();
        setData(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vWebView != null) {
            vWebView.stopLoading();
            vWebView.destroy();
        }
    }

    private void findView() {
        vToolbar = findViewById(R.id.tool_bar);
        vWebView = findViewById(R.id.web_view);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void bindView() {
        //标题栏
        setSupportActionBar(vToolbar);
        vToolbar.setNavigationIcon(R.drawable.ic_action_back);
        vToolbar.setNavigationOnClickListener(view -> finish());
        //WebView
        vWebView.getSettings().setJavaScriptEnabled(true);
        vWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //直接交由当前WebView加载，而不是跳转到外部浏览器
                vWebView.loadUrl(url);
                return true;
            }
        });
    }

    private void setData(Intent intent) {
        String link = intent.getStringExtra(Constant.KEY_WEB_LINK);
        if (TextUtils.isEmpty(link)) {
            ToastUtil.toast(getApplicationContext(), "跳转链接不能为空");
            finish();
            return;
        }
        vWebView.loadUrl(link);
    }

    @Override
    public void onBackPressed() {
        if (vWebView.canGoBack()) {
            vWebView.goBack();
            return;
        }
        super.onBackPressed();
    }
}