package com.quiqueapp.qq;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.faradaj.blurbehind.BlurBehind;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

public class EditProfileActivity extends Activity {

    private int iGender = 0;
    private ImageButton ibMyProfileMale, ibMyProfileFemale, ibMyProfileNoGender;
    private EditText edProfileName, edProfileNick, edProfileBio, edProfilePhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        QQData.m_lActivityList.add(EditProfileActivity.this);

        ibMyProfileMale = (ImageButton) findViewById(R.id.ibMyProfileMale);
        ibMyProfileFemale = (ImageButton) findViewById(R.id.ibMyProfileFemale);
        ibMyProfileNoGender = (ImageButton) findViewById(R.id.ibMyProfileNoGender);

        edProfileName = (EditText) findViewById(R.id.edProfileName);
        edProfileNick = (EditText) findViewById(R.id.edProfileNick);
        edProfileBio = (EditText) findViewById(R.id.edProfileBio);
        edProfilePhone = (EditText) findViewById(R.id.edProfilePhone);

        BlurBehind.getInstance().withAlpha(255).setBackground(this);

        initValues();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_fade_in, R.anim.act_fade_out);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(EditProfileActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(EditProfileActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(EditProfileActivity.this);
        }
    }

    public void initValues() {
        edProfileName.setText(QQData.MyInfo.sName);
        edProfileNick.setText(QQData.MyInfo.sNick);
        edProfileBio.setText(QQData.MyInfo.sBio);
        edProfilePhone.setText(QQData.MyInfo.sPhone);

        if (QQData.MyInfo.iGender == 0)
            chooseNoGender(null);
        else if (QQData.MyInfo.iGender == 1)
            chooseMale(null);
        else if (QQData.MyInfo.iGender == 2)
            chooseFemale(null);
    }

    public void chooseMale(View v) {
        iGender = 1;

        ibMyProfileMale.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_male_selected));
        ibMyProfileFemale.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_female));
        ibMyProfileNoGender.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_nogender));
    }

    public void chooseFemale(View v) {
        iGender = 2;

        ibMyProfileMale.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_male));
        ibMyProfileFemale.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_female_selected));
        ibMyProfileNoGender.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_nogender));
    }

    public void chooseNoGender(View v) {
        iGender = 0;

        ibMyProfileMale.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_male));
        ibMyProfileFemale.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_female));
        ibMyProfileNoGender.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_nogender_selected));
    }

    public void goSaveProfile(View v) {

        final String name = edProfileName.getText().toString().trim();
        final String nick = edProfileNick.getText().toString().trim();
        final String bio = edProfileBio.getText().toString().trim();
        final String phone = edProfilePhone.getText().toString().trim();

        if (name.length() == 0 || nick.length() == 0) {
            QQData.showError(EditProfileActivity.this, "İsim veya kullanıcı adı boş bırakılamaz");
            return;
        }

        if (!QQData.isAlphaNumeric(nick)) {
            QQData.showError(EditProfileActivity.this, "Kullanıcı adı uygun değil");
            return;
        }

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("name", name)
                .add("nick", nick)
                .add("bio", bio)
                .add("phone", phone)
                .add("sex", String.valueOf(iGender));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sUpdateProfileAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_UpdateProfile(httpResponse, name, nick, bio, phone, iGender);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(EditProfileActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_UpdateProfile(HttpResponse httpResponse, String name, String nick, String bio, String phone, int gender) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 901) {   // updated

                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));

                    QQData.MyInfo.sName = name;
                    QQData.MyInfo.sNick = nick;
                    QQData.MyInfo.sBio = bio;
                    QQData.MyInfo.sPhone = phone;
                    QQData.MyInfo.iGender = gender;
                    QQData.m_actHome.fMyProfile.f_updateProfileViews();
                    QQData.m_actHome.fMyProfile.f_refreshMyQuestions();
                    finish();

                } else if (iretval == 900) {   //nick kullaniliyor

                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    QQData.showError(EditProfileActivity.this, "Bu kullanıcı adı daha önceden alınmış");

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error

                QQData.showError(EditProfileActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error

                QQData.m_actHome.doLogout();

            }
        }
    }
}
