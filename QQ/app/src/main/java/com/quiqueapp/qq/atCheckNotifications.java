package com.quiqueapp.qq;

import android.os.AsyncTask;

import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

/**
 * Created by user on 18.2.2016.
 */
public class atCheckNotifications extends AsyncTask<Void, Void, String> {

    protected String doInBackground(Void... params) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(5000);
        httpClient.setMaxRetries(1);
        ParameterMap postparams = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID));
        HttpResponse httpResponse = httpClient.post(QQData.m_sCheckNewNotificationsAdr, postparams);
        if (httpResponse != null)
            return httpResponse.getBodyAsString();
        else
            return "0";
    }

    protected void onPostExecute(String result) {
        if (QQData.m_actHome == null)
            return;

        if (result.equals("1")) {
            QQData.m_actHome.fNotifications.getNewNotifications();
        }
    }
}
