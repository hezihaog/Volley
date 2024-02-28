package com.zh.android.volley.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.github.yutianzuo.curl_native.HttpManager;
import com.github.yutianzuo.curl_native.NetRequester;
import com.github.yutianzuo.curl_native.RequestManager;
import com.github.yutianzuo.curl_native.utils.Misc;
import com.github.yutianzuo.native_crash_handler.NativeCrashHandler;
import com.google.common.reflect.TypeToken;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.zh.android.volley.R;
import com.zh.android.volley.base.BaseFragment;
import com.zh.android.volley.base.BaseSupportActivity;
import com.zh.android.volley.item.HomeArticleItemViewBinder;
import com.zh.android.volley.model.HomeArticleModel;
import com.zh.android.volley.util.GoSharedPreferences;
import com.zh.android.volley.util.ToastUtil;
import com.zh.android.volley.volley.AsyncHttpClientStack;
import com.zh.android.volley.volley.GoHttpClientStack;
import com.zh.android.volley.volley.OkHttpStack;
import com.zh.android.volley.volley.request.FastJsonRequest;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import me.drakeet.multitype.MultiTypeAdapter;

public class HomeFragment extends BaseFragment {
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
    public static final int TYPE_CURL = 7;

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

    private final SharedPreferences mSharedPreferences = GoSharedPreferences.getInstance();

    public static void start(BaseSupportActivity activity, int type) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        args.putInt(HomeFragment.KEY_TYPE, type);
        activity.start(fragment);
    }

    @Override
    public int onInflaterViewId() {
        return R.layout.fragment_home;
    }

    @Override
    public void onBindView(View view) {
        findView(view);
        bindView();
        initVolley();
        initCurl();
        setData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unInitCurl();
        if (mRequestQueue != null) {
            mRequestQueue.stop();
        }
        mSharedPreferences.edit().clear().apply();
    }

    private void findView(View view) {
        vToolbar = view.findViewById(R.id.tool_bar);
        vRefreshLayout = view.findViewById(R.id.refresh_layout);
        vList = view.findViewById(R.id.list);
    }

    private void bindView() {
        //标题栏
        ((AppCompatActivity) requireActivity()).setSupportActionBar(vToolbar);
        vToolbar.setNavigationIcon(R.drawable.ic_action_back);
        vToolbar.setNavigationOnClickListener(view -> getBaseSupportActivity().onBackPressed());
        vList.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
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
                        WebFragment.start(getBaseSupportActivity(), link);
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

    private void initVolley() {
        Context context = getContext().getApplicationContext();
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        int type = args.getInt(KEY_TYPE, TYPE_HTTP_URL_CONNECTION);

        //创建请求队列
        if (type == TYPE_HTTP_URL_CONNECTION) {
            mRequestQueue = Volley.newRequestQueue(context, new HurlStack());
        } else if (type == TYPE_HTTP_CLIENT) {
            mRequestQueue = Volley.newRequestQueue(context, new HttpClientStack(new DefaultHttpClient()));
        } else if (type == TYPE_OKHTTP) {
            mRequestQueue = Volley.newRequestQueue(context, new OkHttpStack(context, false));
        } else if (type == TYPE_OKHTTP_WITH_CRONET) {
            mRequestQueue = Volley.newRequestQueue(context, new OkHttpStack(context, true));
        } else if (type == TYPE_ASYNC_HTTP_CLIENT) {
            mRequestQueue = Volley.newRequestQueue(context, new AsyncHttpClientStack());
        } else if (type == TYPE_GO_HTTP_CLIENT) {
            mRequestQueue = Volley.newRequestQueue(context, new GoHttpClientStack());
        } else {
            mRequestQueue = Volley.newRequestQueue(context, new HurlStack());
        }
    }

    /**
     * 初始化curl
     */
    private void initCurl() {
        Context context = requireActivity().getApplicationContext();
        NetRequester.INSTANCE.init(context);
        //native异常处理器，写日志到目录
        NativeCrashHandler.installNativeCrashHandler(new File(context.getFilesDir(), "android-curl_crash.log").getAbsolutePath());
    }

    /**
     * 解初始化curl
     */
    private void unInitCurl() {
        NetRequester.INSTANCE.unInit();
    }

    @Override
    public void setData() {
        super.setData();
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
        String json = mSharedPreferences.getString(KEY_CACHE_LIST_PREV + page, "");
        if (!TextUtils.isEmpty(json)) {
            finishRefreshOrLoadMore(isRefresh);
            HomeArticleModel response = JSONObject.parseObject(json, type);
            processResult(response, isRefresh);
            return;
        }

        long startTime = System.currentTimeMillis();

        LoadCallback loadCallback = new LoadCallback() {
            @Override
            public void onSuccess(HomeArticleModel response) {
                //缓存数据到内存中
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putString(KEY_CACHE_LIST_PREV + page, JSONObject.toJSONString(response));
                editor.apply();
                //渲染页面
                processResult(response, isRefresh);
            }

            @Override
            public void onError(Exception error) {
                error.printStackTrace();
                ToastUtil.toast(getContext(), "请求失败：" + error.getMessage());
            }

            @Override
            public void onFinish() {
                long endTime = System.currentTimeMillis();
                ToastUtil.toast(getContext(), "完成耗时：" + (endTime - startTime) + "ms");
                finishRefreshOrLoadMore(isRefresh);
            }
        };

        if (isUseCurl()) {
            loadByCurl(url, type, loadCallback);
        } else {
            loadByVolley(url, type, loadCallback);
        }
    }

    public interface LoadCallback {
        void onSuccess(HomeArticleModel response);

        void onError(Exception error);

        void onFinish();
    }

    /**
     * 使用curl发起请求
     */
    private void loadByCurl(String url, Type type, LoadCallback callback) {
        RequestManager requestManager = HttpManager.INSTANCE.getRequest();
        requestManager.setHost(url);
        requestManager.setCertPath(Misc.getAppDir(requireContext()) + Misc.CERT_NAME);

        NetRequester.UrlBuilder builder = new NetRequester.UrlBuilder().with(requestManager);
        builder.get(new NetRequester.HttpResultCallback() {
            @Override
            public void success(com.github.yutianzuo.curl_native.Response data) {
                HomeArticleModel model = JSON.parseObject(data.response, type);
                if (callback != null) {
                    callback.onSuccess(model);
                    callback.onFinish();
                }
            }

            @Override
            public void fail(int errorCode) {
                if (callback != null) {
                    callback.onError(new RuntimeException("请求错误，错误码：" + errorCode));
                    callback.onFinish();
                }
            }
        });
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

    /**
     * 是否使用curl
     */
    private boolean isUseCurl() {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        int type = args.getInt(KEY_TYPE, TYPE_HTTP_URL_CONNECTION);
        return type == TYPE_CURL;
    }
}
