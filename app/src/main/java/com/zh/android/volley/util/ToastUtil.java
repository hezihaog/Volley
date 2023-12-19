package com.zh.android.volley.util;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ToastUtil {
    private ToastUtil() {
    }

    public static void toast(Context context, String msg) {
        Runnable action = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        };
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            //当前就是主线程，直接执行
            action.run();
        } else {
            //其他线程，通过EventHandler发送到主线程中执行
            new Handler(Looper.getMainLooper()).post(action);
        }
    }
}