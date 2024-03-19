package com.zh.android.volley.ui.fragment;

import android.view.View;

import androidx.appcompat.widget.Toolbar;

import com.zh.android.volley.R;
import com.zh.android.volley.base.BaseFragment;
import com.zh.android.volley.base.BaseSupportActivity;

/**
 * 主页
 */
public class MainFragment extends BaseFragment {
    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public int onInflaterViewId() {
        return R.layout.fragment_main;
    }

    @Override
    public void onBindView(View view) {
        BaseSupportActivity activity = getBaseSupportActivity();

        //标题栏
        Toolbar toolbar = view.findViewById(R.id.tool_bar);
        activity.setSupportActionBar(toolbar);

        //HttpUrlConnection实现
        view.findViewById(R.id.get_request_http_url_connection_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeFragment.start(activity, HomeFragment.TYPE_HTTP_URL_CONNECTION);
            }
        });
        //HttpClient实现
        view.findViewById(R.id.get_request_http_client_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeFragment.start(activity, HomeFragment.TYPE_HTTP_CLIENT);
            }
        });
        //OkHttp实现
        view.findViewById(R.id.get_request_okhttp_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeFragment.start(activity, HomeFragment.TYPE_OKHTTP);
            }
        });
        view.findViewById(R.id.get_request_okhttp_btn).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                HomeFragment.start(activity, HomeFragment.TYPE_OKHTTP_WITH_CRONET);
                return true;
            }
        });
        //AsyncHttpClient实现
        view.findViewById(R.id.get_request_async_http_client_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeFragment.start(activity, HomeFragment.TYPE_ASYNC_HTTP_CLIENT);
            }
        });
        //GoHttpClient实现
        View goHttpClientBtn = view.findViewById(R.id.get_request_go_http_client_btn);
        goHttpClientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeFragment.start(activity, HomeFragment.TYPE_GO_HTTP_CLIENT);
            }
        });
        goHttpClientBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                HomeFragment.start(activity, HomeFragment.TYPE_GO_HTTP_CLIENT_ASYNC);
                return true;
            }
        });
        //curl实现
        view.findViewById(R.id.get_request_curl_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeFragment.start(activity, HomeFragment.TYPE_CURL);
            }
        });
    }
}
