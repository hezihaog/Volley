package com.zh.android.volley;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.zh.android.volley.volley.OkHttpStack;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //创建网络适配层
        HttpStack httpStack = new OkHttpStack(getApplicationContext(), true);
        //HttpStack httpStack = new AsyncHttpClientStack();
        //创建请求队列
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext(), httpStack);
        findViewById(R.id.request_get_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //创建请求，设置回调
                StringRequest request = new StringRequest("https://www.wanandroid.com/article/list/1/json", new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, response);
                        toast(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        toast("请求失败：" + error.getMessage());
                        Log.e(TAG, error.getMessage(), error);
                    }
                });
                //加入队列，发起请求
                requestQueue.add(request);
            }
        });
    }

    private void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}