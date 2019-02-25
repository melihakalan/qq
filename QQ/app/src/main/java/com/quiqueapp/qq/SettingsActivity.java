package com.quiqueapp.qq;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.rey.material.widget.Switch;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsActivity extends Activity {

    public final static String sAboutURL = "/help/about";
    public final static String sTOSURL = "/help/tos";
    public final static String sPPURL = "/help/pp";
    public final static String sLicensesURL = "/help/licenses";

    private com.rey.material.widget.Switch swSettingsPrivateAccount, swSettingsToggleNotifications;
    private LinearLayout llSettingsPWLayout, llSettingsReportLayout;

    private EditText edSettingsPW, edSettingsNewPW1, edSettingsNewPW2, edReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        QQData.m_lActivityList.add(SettingsActivity.this);

        swSettingsPrivateAccount = (com.rey.material.widget.Switch) findViewById(R.id.swSettingsPrivateAccount);
        swSettingsToggleNotifications = (com.rey.material.widget.Switch) findViewById(R.id.swSettingsToggleNotifications);
        llSettingsPWLayout = (LinearLayout) findViewById(R.id.llSettingsPWLayout);
        llSettingsReportLayout = (LinearLayout) findViewById(R.id.llSettingsReportLayout);

        edSettingsPW = (EditText) findViewById(R.id.edSettingsPW);
        edSettingsNewPW1 = (EditText) findViewById(R.id.edSettingsNewPW1);
        edSettingsNewPW2 = (EditText) findViewById(R.id.edSettingsNewPW2);
        edReport = (EditText) findViewById(R.id.edReport);

        swSettingsPrivateAccount.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(Switch view, boolean checked) {
                togglePrivate(checked);
            }
        });

        swSettingsToggleNotifications.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(Switch view, boolean checked) {
                toggleNotifications(checked);
            }
        });

        initValues();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_slide_in_right, R.anim.act_slide_out_right);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(SettingsActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(SettingsActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(SettingsActivity.this);
        }
    }

    public void initValues() {

        if (QQData.MyInfo.bPrivate) {
            swSettingsPrivateAccount.setChecked(true);
        }

        if (!QQData.m_bNotifications) {
            swSettingsToggleNotifications.setChecked(false);
        }

    }

    public void togglePWLayout(View v) {
        if (llSettingsPWLayout.getVisibility() == View.VISIBLE) {
            llSettingsPWLayout.setVisibility(View.GONE);
        } else {
            llSettingsPWLayout.setVisibility(View.VISIBLE);
        }
    }

    public void savePassword(View v) {
        String currentPW = edSettingsPW.getText().toString().trim();
        String newPW1 = edSettingsNewPW1.getText().toString().trim();
        String newPW2 = edSettingsNewPW2.getText().toString().trim();

        if (currentPW.length() == 0 || newPW1.length() == 0 || newPW2.length() == 0) {
            QQData.showError(SettingsActivity.this, "Lütfen alanları doldurun");
            return;
        }

        if (currentPW.length() < 8 || !QQData.isAlphaNumeric(currentPW)) {
            QQData.showError(SettingsActivity.this, "Şifreniz minimum 8 karakterden oluşmalı, Türkçe karakter ve boşluk barındırmamalıdır");
            return;
        }

        if (newPW1.length() < 8 || !QQData.isAlphaNumeric(newPW1)) {
            QQData.showError(SettingsActivity.this, "Şifreniz minimum 8 karakterden oluşmalı, Türkçe karakter ve boşluk barındırmamalıdır");
            return;
        }

        if (!newPW1.equals(newPW2)) {
            QQData.showError(SettingsActivity.this, "Şifreler uyuşmuyor");
            return;
        }

        String encryptedCurrentPW = null;
        String encryptedNewPW = null;

        try {
            encryptedCurrentPW = Crypt.bytesToHex(QQData.m_cryptor.encrypt(currentPW));
            encryptedNewPW = Crypt.bytesToHex(QQData.m_cryptor.encrypt(newPW1));
        } catch (Exception e) {
            e.printStackTrace();
        }

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("currentpw", encryptedCurrentPW)
                .add("newpw", encryptedNewPW);

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sChangePWAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_ChangePassword(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(SettingsActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_ChangePassword(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 271) {   // saved
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    QQData.showInfo(SettingsActivity.this, "Şifreniz başarıyla değiştirildi");
                    edSettingsPW.setText("");
                    edSettingsNewPW1.setText("");
                    edSettingsNewPW2.setText("");
                    togglePWLayout(null);
                } else if (iretval == 270) {
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    QQData.showError(SettingsActivity.this, "Mevcut şifreniz yanlış");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(SettingsActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void togglePrivate(boolean checked) {
        postPrivateStatus(checked);
    }

    public void postPrivateStatus(final boolean state) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("state", String.valueOf((state) ? 1 : 0));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSavePrivateStateAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_PostPrivateStatus(httpResponse, state);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(SettingsActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_PostPrivateStatus(HttpResponse httpResponse, final boolean state) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 261) {   // saved
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    QQData.MyInfo.bPrivate = state;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(SettingsActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void goHome(View v) {
        QQData.clearAllActivities();
    }

    public void clickReportProblem(View v) {
        if (llSettingsReportLayout.getVisibility() == View.VISIBLE) {
            llSettingsReportLayout.setVisibility(View.GONE);
        } else {
            llSettingsReportLayout.setVisibility(View.VISIBLE);
        }
    }

    public void clickAbout(View v) {
        Intent it = new Intent(SettingsActivity.this, BrowserActivity.class);
        Bundle b = new Bundle();
        b.putString("info", "Hakkında");
        b.putString("url", QQData.m_sServer + sAboutURL);
        it.putExtras(b);
        Bundle translate = ActivityOptions.makeCustomAnimation(SettingsActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
        startActivity(it, translate);
    }

    public void clickTOS(View v) {
        Intent it = new Intent(SettingsActivity.this, BrowserActivity.class);
        Bundle b = new Bundle();
        b.putString("info", "Kullanım Şartları");
        b.putString("url", QQData.m_sServer + sTOSURL);
        it.putExtras(b);
        Bundle translate = ActivityOptions.makeCustomAnimation(SettingsActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
        startActivity(it, translate);
    }

    public void clickPP(View v) {
        Intent it = new Intent(SettingsActivity.this, BrowserActivity.class);
        Bundle b = new Bundle();
        b.putString("info", "Gizlilik İlkeleri");
        b.putString("url", QQData.m_sServer + sPPURL);
        it.putExtras(b);
        Bundle translate = ActivityOptions.makeCustomAnimation(SettingsActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
        startActivity(it, translate);
    }

    public void clickLicenses(View v) {
        Intent it = new Intent(SettingsActivity.this, BrowserActivity.class);
        Bundle b = new Bundle();
        b.putString("info", "Lisanslar");
        b.putString("url", QQData.m_sServer + sLicensesURL);
        it.putExtras(b);
        Bundle translate = ActivityOptions.makeCustomAnimation(SettingsActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
        startActivity(it, translate);
    }

    public void toggleNotifications(boolean checked) {
        QQData.m_bNotifications = checked;
        QQData.saveUserNotifications();
    }
}
