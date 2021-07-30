package com.android.volley;

/**
 * 重定向异常
 */
public class RedirectError extends VolleyError {
    public RedirectError() {
    }

    public RedirectError(final Throwable cause) {
        super(cause);
    }

    public RedirectError(final NetworkResponse response) {
        super(response);
    }
}