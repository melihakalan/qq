package com.quiqueapp.qq;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.faradaj.blurbehind.BlurBehind;
import com.rey.material.widget.Switch;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

public class UserProfileSettingsActivity extends Activity {

    private int iTargetUID, iBlockState = 0, iSubscribeState = 0, iReportState = 0, iBlockedByUser = 0;
    private ImageButton ibReport;
    private boolean bBlocked = false, bSubscribed = false;
    private com.rey.material.widget.Switch swBlocked, swSubscribed;
    private TextView tvBlocked, tvSubscribed, tvReport;
    private LinearLayout llUserProfileSubscribe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_settings);

        QQData.m_lActivityList.add(UserProfileSettingsActivity.this);

        ibReport = (ImageButton) findViewById(R.id.ibReport);

        swBlocked = (com.rey.material.widget.Switch) findViewById(R.id.swBlocked);
        swSubscribed = (com.rey.material.widget.Switch) findViewById(R.id.swSubscribed);

        tvBlocked = (TextView) findViewById(R.id.tvBlocked);
        tvSubscribed = (TextView) findViewById(R.id.tvSubscribed);
        tvReport = (TextView) findViewById(R.id.tvReport);

        llUserProfileSubscribe = (LinearLayout) findViewById(R.id.llUserProfileSubscribe);

        swBlocked.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(Switch view, boolean checked) {
                changedSWBlocked(checked);
            }
        });

        swSubscribed.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(Switch view, boolean checked) {
                changedSWSubscribed(checked);
            }
        });

        BlurBehind.getInstance().withAlpha(255).setBackground(this);

        getInfo();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_fade_in, R.anim.act_fade_out);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(UserProfileSettingsActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(UserProfileSettingsActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(UserProfileSettingsActivity.this);
        }
    }

    public void getInfo() {

        Bundle b = getIntent().getExtras();
        iTargetUID = b.getInt("target_uid");
        iBlockState = b.getInt("blocked");
        iSubscribeState = b.getInt("subscribed");
        iReportState = b.getInt("reported");
        iBlockedByUser = b.getInt("blockedbyuser");

        if (iBlockedByUser == 1)
            llUserProfileSubscribe.setVisibility(View.GONE);

        if (iBlockState == 1) {
            bBlocked = true;
            swBlocked.setChecked(true);
            tvBlocked.setTypeface(tvBlocked.getTypeface(), Typeface.BOLD);
            tvBlocked.setTextColor(Color.parseColor("#FE2E2E"));
        }

        if (iSubscribeState == 1) {
            bSubscribed = true;
            swSubscribed.setChecked(true);
            tvSubscribed.setTypeface(tvSubscribed.getTypeface(), Typeface.BOLD);
            tvSubscribed.setTextColor(Color.parseColor("#58ACFA"));
        }

        if (iReportState == 1) {
            ibReport.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_report_topbar_sent));
            ibReport.setClickable(false);
            tvReport.setTextColor(Color.parseColor("#585858"));
            tvReport.setText("Şikayet Edildi");
            tvReport.setClickable(false);
        }
    }

    public void changedSWBlocked(boolean checked) {
        if (checked) {
            tvBlocked.setTypeface(tvBlocked.getTypeface(), Typeface.BOLD);
            tvBlocked.setTextColor(Color.parseColor("#FE2E2E"));

            bBlocked = true;
        } else {
            tvBlocked.setTypeface(Typeface.create(tvBlocked.getTypeface(), Typeface.NORMAL));
            tvBlocked.setTextColor(Color.parseColor("#585858"));

            bBlocked = false;
        }
    }

    public void changedSWSubscribed(boolean checked) {
        if (checked) {
            tvSubscribed.setTypeface(tvSubscribed.getTypeface(), Typeface.BOLD);
            tvSubscribed.setTextColor(Color.parseColor("#58ACFA"));

            bSubscribed = true;
        } else {
            tvSubscribed.setTypeface(Typeface.create(tvSubscribed.getTypeface(), Typeface.NORMAL));
            tvSubscribed.setTextColor(Color.parseColor("#585858"));

            bSubscribed = false;
        }
    }

    public void showBlockInfo(View v) {
        QQData.showInfo(UserProfileSettingsActivity.this, "Engellediğiniz kişiler profilinizi göremez, takip edemez ve mesaj gönderemez");
    }

    public void showSubscribeInfo(View v) {
        QQData.showInfo(UserProfileSettingsActivity.this, "Gönderi bildirimlerini açtığınız kişinin paylaşımlarını bildirim olarak alırsınız");
    }

    public void goSaveUserProfile(View v) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("target_uid", String.valueOf(iTargetUID))
                .add("blocked", String.valueOf((bBlocked) ? 1 : 0))
                .add("subscribed", String.valueOf((bSubscribed) ? 1 : 0));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSaveUserStateAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_SaveState(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(UserProfileSettingsActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_SaveState(HttpResponse httpResponse) {
        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 221) {   // save user state

                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    QQData.m_actLastUserProfile.iBlockState = (bBlocked) ? 1 : 0;
                    QQData.m_actLastUserProfile.iSubscribeState = (bSubscribed) ? 1 : 0;
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(UserProfileSettingsActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void goReport(View v) {
        ibReport.setClickable(false);
        tvReport.setClickable(false);

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("target_uid", String.valueOf(iTargetUID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sReportAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_Report(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(UserProfileSettingsActivity.this, "İnternet bağlantısı kurulamadı");
                ibReport.setClickable(true);
                tvReport.setClickable(true);
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_Report(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 241) {   // reported

                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    QQData.m_actLastUserProfile.iReportState = 1;
                    ibReport.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_report_topbar_sent));
                    tvReport.setTextColor(Color.parseColor("#585858"));
                    tvReport.setText("Şikayet Edildi");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(UserProfileSettingsActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }
}
