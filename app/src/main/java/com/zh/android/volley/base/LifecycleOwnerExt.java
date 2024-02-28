package com.zh.android.volley.base;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

/**
 * LifecycleOwner拓展接口
 */
public interface LifecycleOwnerExt extends LifecycleOwner {
    /**
     * 获取FragmentActivity
     */
    FragmentActivity getFragmentActivity();
}