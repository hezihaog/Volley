/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley;

import android.os.Handler;

import java.util.concurrent.Executor;

/**
 * 传递响应和错误
 */
public class ExecutorDelivery implements ResponseDelivery {
    /**
     * 传递响应的线程切换Executor，一般为主线程
     */
    private final Executor mResponsePoster;

    /**
     * 构造方法，传入Handler
     *
     * @param handler 通过这个 {@link Handler} 进行回调
     */
    public ExecutorDelivery(final Handler handler) {
        //创建Executor，它会使用Handler进行执行
        mResponsePoster = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };
    }

    /**
     * 构造方法，传入指定的Executor进行执行和回调
     */
    public ExecutorDelivery(Executor executor) {
        mResponsePoster = executor;
    }

    @Override
    public void postResponse(Request<?> request, Response<?> response) {
        postResponse(request, response, null);
    }

    @Override
    public void postResponse(Request<?> request, Response<?> response, Runnable runnable) {
        request.markDelivered();
        request.addMarker("post-response");
        mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, runnable));
    }

    @Override
    public void postError(Request<?> request, VolleyError error) {
        request.addMarker("post-error");
        Response<?> response = Response.error(error);
        mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, null));
    }

    /**
     * 这个Runnable用于包装，用于在主线程回调
     */
    @SuppressWarnings("rawtypes")
    private static class ResponseDeliveryRunnable implements Runnable {
        private final Request mRequest;
        private final Response mResponse;
        private final Runnable mRunnable;

        public ResponseDeliveryRunnable(Request request, Response response, Runnable runnable) {
            mRequest = request;
            mResponse = response;
            mRunnable = runnable;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            //请求被取消了，不进行回调
            if (mRequest.isCanceled()) {
                mRequest.finish("canceled-at-delivery");
                return;
            }

            //响应成功或失败，这里会回调调用方的监听器
            if (mResponse.isSuccess()) {
                mRequest.deliverResponse(mResponse.result);
            } else {
                mRequest.deliverError(mResponse.error);
            }

            //如果是一个中间响应，标记一下
            if (mResponse.intermediate) {
                mRequest.addMarker("intermediate-response");
            } else {
                //不是中间响应，标记完成
                mRequest.finish("done");
            }

            //执行额外的Runnable
            if (mRunnable != null) {
                mRunnable.run();
            }
        }
    }
}
