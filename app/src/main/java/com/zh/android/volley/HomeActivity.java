package com.zh.android.volley;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSONObject;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.google.common.reflect.TypeToken;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.zh.android.volley.item.HomeArticleItemViewBinder;
import com.zh.android.volley.model.HomeArticleModel;
import com.zh.android.volley.util.GoSharedPreferences;
import com.zh.android.volley.util.ToastUtil;
import com.zh.android.volley.volley.AsyncHttpClientStack;
import com.zh.android.volley.volley.GoHttpClientStack;
import com.zh.android.volley.volley.OkHttpStack;
import com.zh.android.volley.volley.request.FastJsonRequest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import me.drakeet.multitype.MultiTypeAdapter;

public class HomeActivity extends AppCompatActivity {
    public static final String KEY_TYPE = "KEY_TYPE";

    /**
     * 缓存Key前置
     */
    private static final String KEY_CACHE_LIST_PREV = "cache_list_";

    public static final int TYPE_HTTP_URL_CONNECTION = 1;
    public static final int TYPE_HTTP_CLIENT = 2;
    public static final int TYPE_OKHTTP = 3;
    public static final int TYPE_OKHTTP_WITH_CRONET = 4;
    public static final int TYPE_ASYNC_HTTP_CLIENT = 5;
    public static final int TYPE_GO_HTTP_CLIENT = 6;

    private Toolbar vToolbar;
    private SmartRefreshLayout vRefreshLayout;
    private RecyclerView vList;
    private final List<HomeArticleModel.PageModel.ItemModel> mListData = new ArrayList<>();
    private final MultiTypeAdapter mListAdapter = new MultiTypeAdapter(mListData);

    /**
     * 请求队列
     */
    private RequestQueue mRequestQueue;
    /**
     * 当前页码
     */
    private int mCurrentPage;

    private final SharedPreferences mGoSharedPreferences = GoSharedPreferences.getInstance();

    public static void start(Activity activity, int type) {
        Intent intent = new Intent(activity, HomeActivity.class);
        intent.putExtra(HomeActivity.KEY_TYPE, type);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findView();
        bindView();
        initVolley(getIntent());
        setData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRequestQueue != null) {
            mRequestQueue.stop();
        }
        mGoSharedPreferences.edit().clear().apply();
    }

    private void findView() {
        vToolbar = findViewById(R.id.tool_bar);
        vRefreshLayout = findViewById(R.id.refresh_layout);
        vList = findViewById(R.id.list);
    }

    private void bindView() {
        //标题栏
        setSupportActionBar(vToolbar);
        vToolbar.setNavigationIcon(R.drawable.ic_action_back);
        vToolbar.setNavigationOnClickListener(view -> finish());
        vList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        //设置适配器
        vList.setAdapter(mListAdapter);
        //注册条目类型
        mListAdapter.register(HomeArticleModel.PageModel.ItemModel.class,
                //设置点击事件
                new HomeArticleItemViewBinder(new HomeArticleItemViewBinder.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        HomeArticleModel.PageModel.ItemModel itemModel = mListData.get(position);
                        String link = itemModel.getLink();
                        //点击跳转到浏览器
                        WebActivity.start(HomeActivity.this, link);
                    }
                }));
        //下拉刷新
        vRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                refresh();
            }
        });
        //加载更多
        vRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                loadMore();
            }
        });
    }

    private void initVolley(Intent intent) {
        int type = intent.getIntExtra(KEY_TYPE, TYPE_HTTP_URL_CONNECTION);
        //创建请求队列
        if (type == TYPE_HTTP_URL_CONNECTION) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new HurlStack());
        } else if (type == TYPE_HTTP_CLIENT) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new HttpClientStack(new DefaultHttpClient()));
        } else if (type == TYPE_OKHTTP) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new OkHttpStack(getApplicationContext(), false));
        } else if (type == TYPE_OKHTTP_WITH_CRONET) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new OkHttpStack(getApplicationContext(), true));
        } else if (type == TYPE_ASYNC_HTTP_CLIENT) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new AsyncHttpClientStack());
        } else if (type == TYPE_GO_HTTP_CLIENT) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new GoHttpClientStack());
        } else {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new HurlStack());
        }
    }

    private void setData() {
        refresh();
    }

    private void refresh() {
        this.mCurrentPage = 0;
        getHomeArticle(mCurrentPage, true);
    }

    private void loadMore() {
        int nextPage = this.mCurrentPage + 1;
        getHomeArticle(nextPage, false);
    }

    /**
     * 获取首页文章列表
     */
    private void getHomeArticle(int page, boolean isRefresh) {
        String url = "https://www.wanandroid.com/article/list/" + page + "/json";
        Type type = new TypeToken<HomeArticleModel>() {
        }.getType();

        //优先从缓存中获取
        String json = mGoSharedPreferences.getString(KEY_CACHE_LIST_PREV + page, "");
        if (!TextUtils.isEmpty(json)) {
            finishRefreshOrLoadMore(isRefresh);
            HomeArticleModel response = JSONObject.parseObject(json, type);
            processResult(response, isRefresh);
            return;
        }

        long startTime = System.currentTimeMillis();

        loadByVolley(url, type, new LoadCallback() {
            @Override
            public void onSuccess(HomeArticleModel response) {
                //缓存数据到内存中
                SharedPreferences.Editor editor = mGoSharedPreferences.edit();
                editor.putString(KEY_CACHE_LIST_PREV + page, JSONObject.toJSONString(response));
                editor.apply();
                //渲染页面
                processResult(response, isRefresh);
            }

            @Override
            public void onError(Exception error) {
                error.printStackTrace();
                ToastUtil.toast(getApplicationContext(), "请求失败：" + error.getMessage());
            }

            @Override
            public void onFinish() {
                long endTime = System.currentTimeMillis();
                ToastUtil.toast(getApplicationContext(), "完成耗时：" + (endTime - startTime) + "ms");
                finishRefreshOrLoadMore(isRefresh);
            }
        });
    }

    public interface LoadCallback {
        void onSuccess(HomeArticleModel response);

        void onError(Exception error);

        void onFinish();
    }

    /**
     * 使用Volley发起请求
     */
    private void loadByVolley(String url, Type type, LoadCallback callback) {
        //创建请求，设置回调
        Request<HomeArticleModel> request = new FastJsonRequest<HomeArticleModel>(url, type, new Response.Listener<HomeArticleModel>() {
            @Override
            public void onResponse(HomeArticleModel response) {
                if (callback != null) {
                    callback.onSuccess(response);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        }) {
            @Override
            protected void onFinish() {
                super.onFinish();
                if (callback != null) {
                    callback.onFinish();
                }
            }
        };
        //加入队列，发起请求
        mRequestQueue.add(request);
    }

    /**
     * 结束下拉刷新或加载更多
     */
    private void finishRefreshOrLoadMore(boolean isRefresh) {
        if (isRefresh) {
            vRefreshLayout.finishRefresh();
        } else {
            vRefreshLayout.finishLoadMore();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void processResult(HomeArticleModel model, Boolean isRefresh) {
        if (model == null) {
            return;
        }
        HomeArticleModel.PageModel pageModel = model.getData();
        List<HomeArticleModel.PageModel.ItemModel> list;
        if (pageModel.getDatas() != null) {
            list = pageModel.getDatas();
        } else {
            list = new ArrayList<>();
        }
        //没有下一页了
        if (pageModel.isOver()) {
            vRefreshLayout.finishLoadMoreWithNoMoreData();
        }
        //更新当前页码
        mCurrentPage = pageModel.getOffset();
        if (isRefresh) {
            mListData.clear();
        }
        //更新列表
        mListData.addAll(list);
        mListAdapter.notifyDataSetChanged();
    }
}
