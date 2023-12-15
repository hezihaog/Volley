package com.zh.android.volley;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

public class AppContext extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
