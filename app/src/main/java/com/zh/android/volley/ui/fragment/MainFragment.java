package com.zh.android.volley.ui.fragment;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.zh.android.volley.HomeActivity;
import com.zh.android.volley.R;
import com.zh.android.volley.base.BaseFragment;

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
        //标题栏
        Toolbar toolbar = view.findViewById(R.id.tool_bar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        //HttpUrlConnection实现
        view.findViewById(R.id.get_request_http_url_connection_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(getActivity(), HomeActivity.TYPE_HTTP_URL_CONNECTION);
            }
        });
        //HttpClient实现
        view.findViewById(R.id.get_request_http_client_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(getActivity(), HomeActivity.TYPE_HTTP_CLIENT);
            }
        });
        //OkHttp实现
        view.findViewById(R.id.get_request_okhttp_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(getActivity(), HomeActivity.TYPE_OKHTTP);
            }
        });
        view.findViewById(R.id.get_request_okhttp_btn).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                HomeActivity.start(getActivity(), HomeActivity.TYPE_OKHTTP_WITH_CRONET);
                return true;
            }
        });
        //AsyncHttpClient实现
        view.findViewById(R.id.get_request_async_http_client_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(getActivity(), HomeActivity.TYPE_ASYNC_HTTP_CLIENT);
            }
        });
        //GoHttpClient实现
        view.findViewById(R.id.get_request_go_http_client_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(getActivity(), HomeActivity.TYPE_GO_HTTP_CLIENT);
            }
        });
        //curl实现
        view.findViewById(R.id.get_request_curl_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.start(getActivity(), HomeActivity.TYPE_CURL);
            }
        });
    }
}
