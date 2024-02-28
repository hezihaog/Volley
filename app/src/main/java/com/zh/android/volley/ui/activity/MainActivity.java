package com.zh.android.volley.ui.activity;

import android.view.View;

import com.zh.android.volley.R;
import com.zh.android.volley.base.BaseActivity;
import com.zh.android.volley.ui.fragment.MainFragment;

/**
 * 主页
 */
public class MainActivity extends BaseActivity {
    @Override
    public int onInflaterViewId() {
        return R.layout.activity_main;
    }

    @Override
    public void onBindView(View view) {
        if (findFragment(MainFragment.class) == null) {
            loadRootFragment(R.id.main_root, MainFragment.newInstance());
        }
    }
}