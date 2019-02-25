package com.quiqueapp.qq;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.dd.processbutton.iml.ActionProcessButton;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import net.danlew.android.joda.JodaTimeAndroid;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Random;


public class LoginActivity extends Activity {

    private String sEmail = null;

    CallbackManager callbackManager;    //Facebook
    private String sFB_Token = null, sFB_UID = null, sFB_Name = null, sFB_LastName = null, sFB_Email = null;

    private ActionProcessButton btLoginMail, btLoginAccount, btSignUp, btFacebookGetNick;
    private EditText edLoginMail, edLoginPW, edSignupNick, edSignupName, edSignupPW, edSignupPW2, edFacebookGetNick;
    private LinearLayout llLoginBackground, llLoginLogo, llLoginFirst, llLoginPW, llSignUp, llLoginFacebook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        JodaTimeAndroid.init(getApplicationContext());
        QQData.initData();
        QQData.m_ctxAPP = getApplicationContext();
        if (!ImageLoader.getInstance().isInited())
            QQData.initUIL();

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        if (loginResult.getAccessToken() != null) {

                            sFB_Token = loginResult.getAccessToken().getToken();
                            sFB_UID = loginResult.getAccessToken().getUserId();
                            System.out.println("FB Token: " + sFB_Token);
                            GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

                                @Override
                                public void onCompleted(JSONObject object, GraphResponse response) {
                                    getFacebookData(object);
                                }
                            });

                            Bundle parameters = new Bundle();
                            parameters.putString("fields", "first_name, last_name, email");
                            request.setParameters(parameters);
                            request.executeAsync();

                        }
                    }

                    @Override
                    public void onCancel() {
                        // App code
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                    }
                });


        btLoginMail = (ActionProcessButton) findViewById(R.id.btLoginMail);
        btLoginAccount = (ActionProcessButton) findViewById(R.id.btLoginAccount);
        btSignUp = (ActionProcessButton) findViewById(R.id.btSignUp);
        btFacebookGetNick = (ActionProcessButton) findViewById(R.id.btFacebookGetNick);

        btLoginMail.setMode(ActionProcessButton.Mode.ENDLESS);
        btLoginAccount.setMode(ActionProcessButton.Mode.ENDLESS);
        btSignUp.setMode(ActionProcessButton.Mode.ENDLESS);
        btFacebookGetNick.setMode(ActionProcessButton.Mode.ENDLESS);

        edLoginMail = (EditText) findViewById(R.id.edLoginMail);
        edLoginPW = (EditText) findViewById(R.id.edLoginPW);
        edSignupNick = (EditText) findViewById(R.id.edSignupNick);
        edSignupName = (EditText) findViewById(R.id.edSignupName);
        edSignupPW = (EditText) findViewById(R.id.edSignupPW);
        edSignupPW2 = (EditText) findViewById(R.id.edSignupPW2);
        edFacebookGetNick = (EditText) findViewById(R.id.edFacebookGetNick);

        llLoginBackground = (LinearLayout) findViewById(R.id.llLoginBackground);
        llLoginLogo = (LinearLayout) findViewById(R.id.llLoginLogo);
        llLoginFirst = (LinearLayout) findViewById(R.id.llLoginFirst);
        llLoginPW = (LinearLayout) findViewById(R.id.llLoginPW);
        llSignUp = (LinearLayout) findViewById(R.id.llSignUp);
        llLoginFacebook = (LinearLayout) findViewById(R.id.llLoginFacebook);

        Animation fadein = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.anim_fade_in);
        fadein.setDuration(3000);
        llLoginLogo.startAnimation(fadein);
        llLoginFirst.startAnimation(fadein);

        checkSavedUser();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_slide_in_right_home, R.anim.act_slide_out_right);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void facebookLogin(View v) {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email", "user_friends"));
    }

    private void getFacebookData(JSONObject object) {
        try {
            sFB_Name = object.getString("first_name");
            sFB_LastName = object.getString("last_name");
            sFB_Email = object.getString("email");

            if (sFB_Email == null) {
                QQData.showError(LoginActivity.this, "Facebook hesabınızda kayıtlı bir E-Mail olmalıdır");
                return;
            }

            sEmail = sFB_Email;
            checkRegistered_FBTW(sFB_Email, 1);

        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
    }

    public void checkRegistered_FBTW(final String mail, final int type) {

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams().add("email", mail);

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sCheckMailAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {

                String sRet = httpResponse.getBodyAsString();
                if (QQData.isJSON(sRet)) {

                    try {
                        JSONObject jsonRet = new JSONObject(sRet);
                        String retval = jsonRet.getString("ret");
                        int iretval = Integer.parseInt(retval);

                        if (iretval == 101) {   // e-mail kayıtlı

                            if (type == 1)   //FB
                            {
                                loginWithFB(mail);
                            } else {         //TW

                            }

                        } else if (iretval == 100) {  // e-mail yok

                            if (type == 1)   //FB
                            {
                                llLoginFirst.setVisibility(View.GONE);
                                llLoginFacebook.setVisibility(View.VISIBLE);
                            } else {         //TW

                            }

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {

                    int ret = Integer.parseInt(sRet);
                    if (ret == -1) {   // db / param error
                        QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");
                    }
                }

            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void loginWithFB(String mail) {

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams().add("email", mail).add("fbuid", sFB_UID).add("fbtoken", sFB_Token);

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sFBLoginAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_LoginFB(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_LoginFB(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 131) {   // login başarılı

                    String sid = jsonRet.getString("sid");
                    String uid = jsonRet.getString("uid");

                    QQData.m_iSID = Integer.parseInt(sid);
                    QQData.m_iUID = Integer.parseInt(uid);
                    QQData.m_bFBAccount = true;

                    saveUser();
                    startHomeActivity();

                } else if (iretval == 130) {  // kayıt yok

                    QQData.showError(LoginActivity.this, "Böyle bir Facebook kullanıcısı bulunamadı");

                } else if (iretval == 132) {   // banned

                    QQData.showError(LoginActivity.this, "Hesabınız kapatılmıştır");

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error

                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");

            }
        }
    }

    public void goLoginEmail(View v) {
        if (edLoginMail.getText().toString().trim().length() == 0) {
            QQData.showError(LoginActivity.this, "E-Mail adresi giriniz");
            return;
        }

        final String mail = edLoginMail.getText().toString().trim();
        if (!mail.contains("@") || mail.contains(" ")) {
            QQData.showError(LoginActivity.this, "E-Mail adresini düzgün giriniz");
            return;
        }

        btLoginMail.setProgress(1);
        btLoginMail.setClickable(false);

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams().add("email", mail).add("SO_TIMEOUT", String.valueOf(10000));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sCheckMailAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                btLoginMail.setClickable(true);
                btLoginMail.setProgress(0);

                getResult_LoginEmail(httpResponse, mail);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");
                btLoginMail.setClickable(true);
                btLoginMail.setProgress(0);
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_LoginEmail(HttpResponse httpResponse, String mail) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 101) {   // e-mail kayıtlı

                    sEmail = mail;
                    llLoginFirst.setVisibility(View.GONE);
                    llLoginPW.setVisibility(View.VISIBLE);

                } else if (iretval == 100) {  // e-mail yok

                    sEmail = mail;
                    llLoginFirst.setVisibility(View.GONE);
                    llSignUp.setVisibility(View.VISIBLE);

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error

                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");

            }
        }
    }

    public void goLoginAccount(View v) {
        if (sEmail == null || edLoginPW.getText().toString().trim().length() == 0) {
            QQData.showError(LoginActivity.this, "Şifre giriniz");
            return;
        }

        if (edLoginPW.getText().toString().trim().length() < 8) {
            QQData.showError(LoginActivity.this, "Şifre minimum 8 karakterden oluşmalıdır");
            return;
        }

        final String pw = edLoginPW.getText().toString().trim();

        if (!QQData.isAlphaNumeric(pw)) {
            QQData.showError(LoginActivity.this, "Şifrenizde boşluk, Türkçe vb. karakterler olmamalıdır");
            return;
        }

        String encryptedPW = null;

        try {
            encryptedPW = Crypt.bytesToHex(QQData.m_cryptor.encrypt(pw));
            System.out.println(pw);
            System.out.println(encryptedPW);
        } catch (Exception e) {
            e.printStackTrace();
        }

        btLoginAccount.setProgress(1);
        btLoginAccount.setClickable(false);

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams().add("email", sEmail).add("pw", encryptedPW);

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sLoginAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                btLoginAccount.setClickable(true);
                btLoginAccount.setProgress(0);

                getResult_LoginAccount(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");
                btLoginAccount.setClickable(true);
                btLoginAccount.setProgress(0);
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_LoginAccount(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 111) {   // login başarılı

                    String sid = jsonRet.getString("sid");
                    String uid = jsonRet.getString("uid");

                    QQData.m_iSID = Integer.parseInt(sid);
                    QQData.m_iUID = Integer.parseInt(uid);

                    saveUser();
                    startHomeActivity();

                } else if (iretval == 110) {  // kayıt yok

                    QQData.showError(LoginActivity.this, "Şifre yanlış");

                } else if (iretval == 112) {   // banned

                    QQData.showError(LoginActivity.this, "Hesabınız kapatılmıştır");

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error

                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");

            }
        }
    }

    public void goSignUp(View v) {
        if (sEmail == null) {
            QQData.showError(LoginActivity.this, "E-Mail girilmemiş");
            return;
        }

        final String nick = edSignupNick.getText().toString().trim();
        final String name = edSignupName.getText().toString().trim();
        final String pw = edSignupPW.getText().toString().trim();
        final String pw2 = edSignupPW2.getText().toString().trim();

        if (nick.length() == 0 || name.length() == 0 || pw.length() == 0 || pw2.length() == 0) {
            QQData.showError(LoginActivity.this, "Lütfen bilgileri eksiksiz giriniz");
            return;
        }

        if (!QQData.isAlphaNumeric(nick)) {
            QQData.showError(LoginActivity.this, "Kullanıcı adınızda boşluk, Türkçe vb. karakterler olmamalıdır");
            return;
        }

        if (pw.length() < 8 || !QQData.isAlphaNumeric(pw)) {
            QQData.showError(LoginActivity.this, "Şifreniz minimum 8 karakterden oluşmalı ve boşluk, Türkçe vb. karakterler barındırmamalıdır");
            return;
        }

        if (!pw.equals(pw2)) {
            QQData.showError(LoginActivity.this, "Şifreler aynı değil");
            return;
        }

        String encryptedPW = null;

        try {
            encryptedPW = Crypt.bytesToHex(QQData.m_cryptor.encrypt(pw));
        } catch (Exception e) {
            e.printStackTrace();
        }

        btSignUp.setProgress(1);
        btSignUp.setClickable(false);

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams().add("email", sEmail).add("nick", nick).add("name", name).add("pw", encryptedPW);

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSignupAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                btSignUp.setClickable(true);
                btSignUp.setProgress(0);

                getResult_SignUp(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");
                btSignUp.setClickable(true);
                btSignUp.setProgress(0);
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_SignUp(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 121) {   // signup başarılı

                    String sid = jsonRet.getString("sid");
                    String uid = jsonRet.getString("uid");

                    QQData.m_iSID = Integer.parseInt(sid);
                    QQData.m_iUID = Integer.parseInt(uid);

                    saveUser();
                    startHomeActivity();

                } else if (iretval == 120) {  // nick/e-mail kayıtlı

                    QQData.showError(LoginActivity.this, "Bu kullanıcı adı daha önceden alınmış");

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error

                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");

            }
        }
    }

    public void startHomeActivity() {
        Intent it = new Intent(LoginActivity.this, HomeActivity.class);
        Bundle translate = ActivityOptions.makeCustomAnimation(LoginActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();

        Bundle params = getIntent().getExtras();
        if (params != null && !params.isEmpty()) {
            it.putExtras(params);
        }

        startActivity(it, translate);
        finish();
    }

    public void goForgotPW(View v) {

    }

    public void goFacebookSignup(View v) {
        final String nick = edFacebookGetNick.getText().toString().trim();

        if (nick.length() == 0) {
            QQData.showError(LoginActivity.this, "Kullanıcı adını giriniz");
            return;
        }

        if (!QQData.isAlphaNumeric(nick)) {
            QQData.showError(LoginActivity.this, "Kullanıcı adınızda boşluk, Türkçe vb. karakterler olmamalıdır");
            return;
        }

        String PW = String.valueOf(new Random().nextInt(999999999) + 100000000);
        String encryptedPW = null;

        try {
            encryptedPW = Crypt.bytesToHex(QQData.m_cryptor.encrypt(PW));
        } catch (Exception e) {
            e.printStackTrace();
        }

        btFacebookGetNick.setProgress(1);
        btFacebookGetNick.setClickable(false);

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams().add("email", sFB_Email).add("nick", nick).add("name", sFB_Name + " " + sFB_LastName).add("pw", encryptedPW);

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSignupAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                btFacebookGetNick.setClickable(true);
                btFacebookGetNick.setProgress(0);

                getResult_FacebookSignUp(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");
                btFacebookGetNick.setClickable(true);
                btFacebookGetNick.setProgress(0);
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_FacebookSignUp(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 121) {   // signup başarılı

                    String sid = jsonRet.getString("sid");
                    String uid = jsonRet.getString("uid");

                    QQData.m_iSID = Integer.parseInt(sid);
                    QQData.m_iUID = Integer.parseInt(uid);
                    QQData.m_bFBAccount = true;

                    saveUser();
                    startHomeActivity();

                } else if (iretval == 120) {  // nick/e-mail kayıtlı

                    QQData.showError(LoginActivity.this, "Bu kullanıcı adı daha önceden alınmış");

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error

                QQData.showError(LoginActivity.this, "İnternet bağlantısı kurulamadı");

            }
        }
    }

    public void saveUser() {

        SecurePreferences SPFile = new SecurePreferences(QQData.m_ctxAPP, QQData.m_sDataSPName, QQData.m_sSPKey, true);

        SPFile.put("uid", String.valueOf(QQData.m_iUID));
        SPFile.put("sid", String.valueOf(QQData.m_iSID));
        SPFile.put("fbaccount", String.valueOf((QQData.m_bFBAccount) ? 1 : 0));
        SPFile.put("notifications", "1");
    }

    public void checkSavedUser() {

        SecurePreferences SPFile = new SecurePreferences(QQData.m_ctxAPP, QQData.m_sDataSPName, QQData.m_sSPKey, true);

        if (SPFile.getString("uid") != null) {

            QQData.m_iUID = Integer.parseInt(SPFile.getString("uid"));
            QQData.m_iSID = Integer.parseInt(SPFile.getString("sid"));

            int iFBAccount = Integer.parseInt(SPFile.getString("fbaccount"));
            if (iFBAccount == 1)
                QQData.m_bFBAccount = true;

            if (SPFile.containsKey("notifications")) {
                int iNotifications = Integer.parseInt(SPFile.getString("notifications"));
                if (iNotifications == 0)
                    QQData.m_bNotifications = false;
            }

            startHomeActivity();
        }

    }
}
