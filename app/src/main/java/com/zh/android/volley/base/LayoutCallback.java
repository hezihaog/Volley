package com.zh.android.volley.base;

import android.view.View;

/**
 * UI界面加载以及事件
 */
public interface LayoutCallback {
    /**
     * 设置布局之前回调
     */
    void onLayoutBefore();

    /**
     * 获取布局LayoutId
     */
    int onInflaterViewId();

    /**
     * 填充完毕View后回调
     */
    void onInflaterViewAfter(View view);

    /**
     * 查找View和给View进行相关设置等
     */
    void onBindView(View view);

    /**
     * 设置数据
     */
    void setData();
}