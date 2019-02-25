package com.quiqueapp.qq;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

public class BrowserActivity extends Activity {

    private String sInfo = null, sUrl = null;
    private TextView tvBrowserInfo;
    private WebView wvBrowser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        QQData.m_lActivityList.add(BrowserActivity.this);

        tvBrowserInfo = (TextView) findViewById(R.id.tvBrowserInfo);
        wvBrowser = (WebView) findViewById(R.id.wvBrowser);

        Bundle b = getIntent().getExtras();
        sInfo = b.getString("info");
        sUrl = b.getString("url");

        tvBrowserInfo.setText(sInfo);
        wvBrowser.loadUrl(sUrl);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_slide_in_right, R.anim.act_slide_out_right);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(BrowserActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(BrowserActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(BrowserActivity.this);
        }
    }

    public void goHome(View v) {
        QQData.clearAllActivities();
    }
}
