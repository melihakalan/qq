package com.quiqueapp.qq;

import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

/**
 * Created by user on 22.12.2015.
 */
public class PostRequest {
    public AndroidHttpClient client;
    public ParameterMap pm;
    public String adr;
    public AsyncCallback callback;

    public PostRequest(AndroidHttpClient client, ParameterMap pm, String adr, AsyncCallback callback) {
        this.client = client;
        this.pm = pm;
        this.adr = adr;
        this.callback = callback;
    }
}
