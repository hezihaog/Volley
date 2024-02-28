package com.zh.android.volley.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Fragment基类
 */
public abstract class BaseFragment extends BaseSupportFragment implements LayoutCallback, LifecycleOwnerExt {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onLayoutBefore();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layoutId = onInflaterViewId();
        if (layoutId == -1) {
            return null;
        }
        View rootView = inflater.inflate(layoutId, container, false);
        onInflaterViewAfter(rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!isNeedLazy()) {
            onBindView(getView());
            setData();
        }
    }

    @Override
    public void onLazyInitView(@Nullable Bundle savedInstanceState) {
        super.onLazyInitView(savedInstanceState);
        if (isNeedLazy()) {
            onBindView(getView());
            setData();
        }
    }

    protected boolean isNeedLazy() {
        return true;
    }

    @Override
    public void onInflaterViewAfter(View view) {
    }

    @Override
    public void onLayoutBefore() {
    }

    @Override
    public void setData() {
    }

    @Override
    public FragmentActivity getFragmentActivity() {
        return getActivity();
    }

    public LifecycleOwnerExt getLifecycleOwner() {
        return this;
    }
}
