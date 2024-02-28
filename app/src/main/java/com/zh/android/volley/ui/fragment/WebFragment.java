package com.zh.android.volley.ui.fragment;

import static android.os.Build.VERSION_CODES.KITKAT;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.widget.Toolbar;

import com.zh.android.volley.Constant;
import com.zh.android.volley.R;
import com.zh.android.volley.base.BaseFragment;
import com.zh.android.volley.base.BaseSupportActivity;
import com.zh.android.volley.ui.widget.WebProgress;
import com.zh.android.volley.util.ToastUtil;

/**
 * WebView页面
 */
public class WebFragment extends BaseFragment {
    private Toolbar vToolbar;
    private WebView vWebView;
    private WebProgress vProgress;

    public static void start(BaseSupportActivity activity, String link) {
        WebFragment fragment = new WebFragment();
        Bundle args = new Bundle();
        args.putString(Constant.KEY_WEB_LINK, link);
        fragment.setArguments(args);
        activity.start(fragment);
    }

    @Override
    public int onInflaterViewId() {
        return R.layout.fragment_web;
    }

    @Override
    public void onBindView(View view) {
        findView(view);
        bindView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (vWebView != null) {
            vWebView.stopLoading();
            vWebView.destroy();
        }
    }

    private void findView(View view) {
        vToolbar = view.findViewById(R.id.tool_bar);
        vWebView = view.findViewById(R.id.web_view);
        vProgress = view.findViewById(R.id.progress);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void bindView() {
        //标题栏
        getBaseSupportActivity().setSupportActionBar(vToolbar);
        vToolbar.setNavigationIcon(R.drawable.ic_action_back);
        vToolbar.setNavigationOnClickListener(view -> getBaseSupportActivity().onBackPressed());
        //进度条
        initWebProgress();
        //WebView
        initWebViewSettings();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewSettings() {
        WebSettings ws = vWebView.getSettings();
        // 网页内容的宽度是否可大于WebView控件的宽度
        ws.setLoadWithOverviewMode(false);
        // 保存表单数据
        ws.setSaveFormData(true);
        // 是否应该支持使用其屏幕缩放控件和手势缩放
        ws.setSupportZoom(true);
        // 设置内置的缩放控件。若为false，则该WebView不可缩放
        ws.setBuiltInZoomControls(true);
        // 隐藏原生的缩放控件
        ws.setDisplayZoomControls(false);
        // 启动应用缓存
        ws.setAppCacheEnabled(true);
        // 设置缓存模式
        // 缓存模式如下：
        // LOAD_CACHE_ONLY: 不使用网络，只读取本地缓存数据
        // LOAD_DEFAULT: （默认）根据cache-control决定是否从网络上取数据。
        // LOAD_NO_CACHE: 不使用缓存，只从网络获取数据.
        // LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setAppCacheMaxSize(Long.MAX_VALUE);
        // setDefaultZoom  api19被弃用
        // 设置此属性，可任意比例缩放。
        ws.setUseWideViewPort(true);
        // 告诉WebView启用JavaScript执行。默认的是false。
        // 注意：这个很重要   如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        ws.setJavaScriptEnabled(true);
        //  页面加载好以后，再放开图片
        //ws.setBlockNetworkImage(false);
        // 使用localStorage则必须打开
        ws.setDomStorageEnabled(true);
        //防止中文乱码
        ws.setDefaultTextEncodingName("UTF-8");
        /*
         * 排版适应屏幕
         * 用WebView显示图片，可使用这个参数
         * 设置网页布局类型： 1、LayoutAlgorithm.NARROW_COLUMNS ：
         * 适应内容大小 2、LayoutAlgorithm.SINGLE_COLUMN:适应屏幕，内容将自动缩放
         */
        ws.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        // WebView是否新窗口打开(加了后可能打不开网页)
        //ws.setSupportMultipleWindows(true);
        // webview从5.0开始默认不允许混合模式,https中不能加载http资源,需要设置开启。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        //设置字体默认缩放大小
        ws.setTextZoom(100);
        // 不缩放
        vWebView.setInitialScale(100);
        if (Build.VERSION.SDK_INT >= KITKAT) {
            //设置网页在加载的时候暂时不加载图片
            ws.setLoadsImagesAutomatically(true);
        } else {
            ws.setLoadsImagesAutomatically(false);
        }
        //默认关闭硬件加速
        setOpenLayerType(false);
        //默认不开启密码保存功能
        setSavePassword(false);
        //移除高危风险js监听
        setRemoveJavascriptInterface();
        vWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                //获取到网页标题，更新Toolbar的标题
                vToolbar.setTitle(title);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                //更新进度条
                vProgress.setWebProgress(newProgress);
            }
        });
        vWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //直接交由当前WebView加载，而不是跳转到外部浏览器
                vWebView.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //隐藏进度条
                vProgress.hide();
                vProgress.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 初始化进度条
     */
    private void initWebProgress() {
        vProgress.setColor(R.color.purple_700);
        vProgress.show();
    }

    /**
     * 是否开启软硬件加速
     *
     * @param layerType 布尔值
     */
    public void setOpenLayerType(boolean layerType) {
        if (layerType) {
            //开启软硬件加速，开启软硬件加速这个性能提升还是很明显的，但是会耗费更大的内存 。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                vWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                vWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                vWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }
    }

    /**
     * WebView 默认开启密码保存功能，但是存在漏洞。
     * 如果该功能未关闭，在用户输入密码时，会弹出提示框，询问用户是否保存密码，如果选择”是”，
     * 密码会被明文保到 /data/data/com.package.name/databases/webview.db 中，这样就有被盗取密码的危险
     */
    public void setSavePassword(boolean save) {
        vWebView.getSettings().setSavePassword(save);
    }

    /**
     * 在4.2之前，js存在漏洞。不过现在4.2的手机很少了
     */
    private void setRemoveJavascriptInterface() {
        vWebView.removeJavascriptInterface("searchBoxJavaBridge_");
        vWebView.removeJavascriptInterface("accessibility");
        vWebView.removeJavascriptInterface("accessibilityTraversal");
    }

    @Override
    public void setData() {
        super.setData();
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        String link = args.getString(Constant.KEY_WEB_LINK);
        if (link == null || TextUtils.isEmpty(link)) {
            ToastUtil.toast(getContext(), "跳转链接不能为空");
            getBaseSupportActivity().onBackPressed();
            return;
        }
        vWebView.loadUrl(link);
    }

    @Override
    public boolean onBackPressedSupport() {
        if (vWebView.canGoBack()) {
            vWebView.goBack();
            return true;
        }
        return super.onBackPressedSupport();
    }
}