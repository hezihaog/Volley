package com.zh.android.volley;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.google.common.reflect.TypeToken;
import com.zh.android.volley.item.HomeArticleItemViewBinder;
import com.zh.android.volley.model.HomeArticleModel;
import com.zh.android.volley.util.ToastUtil;
import com.zh.android.volley.volley.AsyncHttpClientStack;
import com.zh.android.volley.volley.GoHttpClientStack;
import com.zh.android.volley.volley.OkHttpStack;
import com.zh.android.volley.volley.request.JacksonRequest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import me.drakeet.multitype.MultiTypeAdapter;

public class HomeActivity extends AppCompatActivity {
    public static final String KEY_TYPE = "KEY_TYPE";

    public static final int TYPE_HTTP_URL_CONNECTION = 1;
    public static final int TYPE_HTTP_CLIENT = 2;
    public static final int TYPE_OKHTTP = 3;
    public static final int TYPE_ASYNC_HTTP_CLIENT = 4;
    public static final int TYPE_GO_HTTP_CLIENT = 5;

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
    /**
     * 是否有下一页
     */
    private boolean hasMore = true;

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
    }

    private void findView() {
        vList = findViewById(R.id.list);
    }

    private void bindView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        vList.setLayoutManager(layoutManager);
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
        //设置滚动监听，滚动结束后回调
        vList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //没有下一页
                if (!hasMore) {
                    return;
                }
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                if (lastVisibleItemPosition == mListData.size() - 1) {
                    //加载更多
                    loadMore();
                }
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

        long startTime = System.currentTimeMillis();

        //创建请求，设置回调
        Request<HomeArticleModel> request = new JacksonRequest<HomeArticleModel>(url, type, new Response.Listener<HomeArticleModel>() {
            @Override
            public void onResponse(HomeArticleModel response) {
                processResult(response, isRefresh);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                ToastUtil.toast(getApplicationContext(), "请求失败：" + error.getMessage());
            }
        }) {
            @Override
            protected void onFinish() {
                super.onFinish();
                long endTime = System.currentTimeMillis();
                ToastUtil.toast(getApplicationContext(), "完成耗时：" + (endTime - startTime) + "ms");
            }
        };
        //加入队列，发起请求
        mRequestQueue.add(request);
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
        //是否有下一页
        hasMore = !pageModel.isOver();
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
