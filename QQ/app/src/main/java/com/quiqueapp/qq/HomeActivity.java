package com.quiqueapp.qq;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.facebook.login.LoginManager;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.Timer;
import java.util.TimerTask;

public class HomeActivity extends Activity {

    private boolean bLogoutProcess = false;

    public boolean bActivityPaused = false;
    public Timer tmNotifications;
    public boolean bServiceActive = false;

    private class ttCheckNotifications extends TimerTask {
        public void run() {
            if (QQData.isAppVisible()) {
                if (bServiceActive) {
                    bServiceActive = false;
                    stopNotificationsService();
                }
                new atCheckNotifications().execute();
            } else {
                if (!bServiceActive && QQData.m_bNotifications) {
                    bServiceActive = true;
                    startNotificationsService();
                }
            }
        }
    }

    public F_Home fHome = new F_Home();
    public F_Search fSearch = new F_Search();
    public F_PostQuestion fPostQuestion = new F_PostQuestion();
    public F_Notifications fNotifications = new F_Notifications();
    public F_MyProfile fMyProfile = new F_MyProfile();

    public ImageButton ibLogout, ibSettings, ibChat, ibFav, ibNearQuestions;

    private Fragment[] fragmentSet = {fHome, fSearch, fPostQuestion, fNotifications, fMyProfile};
    private LinearLayout[] llToolbarSet = new LinearLayout[5];
    public ImageButton[] ibToolbarSet = new ImageButton[5];

    private Integer[] iIconSetOff = {R.drawable.toolbar_selector_1, R.drawable.toolbar_selector_2, R.drawable.toolbar_selector_3, R.drawable.toolbar_selector_4, R.drawable.toolbar_selector_5};
    private Integer[] iIconSetOn = {R.drawable.ic_toolbar_home2, R.drawable.ic_toolbar_search2, R.drawable.ic_toolbar_create, R.drawable.ic_toolbar_notifications2, R.drawable.ic_toolbar_myprofile2};

    private int iCurrentFragment = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        QQData.m_actHome = this;

        ibLogout = (ImageButton) findViewById(R.id.ibLogout);
        ibSettings = (ImageButton) findViewById(R.id.ibSettings);
        ibChat = (ImageButton) findViewById(R.id.ibChat);
        ibFav = (ImageButton) findViewById(R.id.ibFav);
        ibNearQuestions = (ImageButton) findViewById(R.id.ibNearQuestions);

        llToolbarSet[0] = (LinearLayout) findViewById(R.id.llToolbar1);
        llToolbarSet[1] = (LinearLayout) findViewById(R.id.llToolbar2);
        llToolbarSet[2] = (LinearLayout) findViewById(R.id.llToolbar3);
        llToolbarSet[3] = (LinearLayout) findViewById(R.id.llToolbar4);
        llToolbarSet[4] = (LinearLayout) findViewById(R.id.llToolbar5);

        ibToolbarSet[0] = (ImageButton) findViewById(R.id.ibToolbar1);
        ibToolbarSet[1] = (ImageButton) findViewById(R.id.ibToolbar2);
        ibToolbarSet[2] = (ImageButton) findViewById(R.id.ibToolbar3);
        ibToolbarSet[3] = (ImageButton) findViewById(R.id.ibToolbar4);
        ibToolbarSet[4] = (ImageButton) findViewById(R.id.ibToolbar5);

        loadFragments();

        tmNotifications = new Timer();
        tmNotifications.schedule(new ttCheckNotifications(), 10000, 10000);

        stopNotificationsService();

        checkParams();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_slide_in_right_home, R.anim.act_slide_out_right);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing()) {
            if (tmNotifications != null) {
                tmNotifications.cancel();
                tmNotifications.purge();
                tmNotifications = null;
            }

            if (bLogoutProcess)
                stopNotificationsService();
            else if (QQData.m_bNotifications)
                startNotificationsService();
        }

        bActivityPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        bActivityPaused = false;
    }

    public void loadFragments() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        for (int i = 0; i < fragmentSet.length; i++) {
            fragmentTransaction.add(R.id.llHomeContainer, fragmentSet[i]);
            fragmentTransaction.hide(fragmentSet[i]);
        }

        fragmentTransaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();

        // Home fragment göster
        setFragment(0);
    }

    public void toolbarClick1(View v) {
        setFragment(0);
    }

    public void toolbarClick2(View v) {
        setFragment(1);
    }

    public void toolbarClick3(View v) {
        setFragment(2);
    }

    public void toolbarClick4(View v) {
        setFragment(3);
    }

    public void toolbarClick5(View v) {
        setFragment(4);
    }

    public void setFragment(int idx) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.hide(fragmentSet[iCurrentFragment]);
        fragmentTransaction.show(fragmentSet[idx]);
        fragmentTransaction.commitAllowingStateLoss();

        ibToolbarSet[iCurrentFragment].setBackgroundDrawable(getResources().getDrawable(iIconSetOff[iCurrentFragment]));
        ibToolbarSet[idx].setBackgroundDrawable(getResources().getDrawable(iIconSetOn[idx]));

        llToolbarSet[iCurrentFragment].setBackgroundDrawable(getResources().getDrawable(R.drawable.toolbar_color));
        llToolbarSet[idx].setBackgroundColor(getResources().getColor(R.color.toolbar_on));

        iCurrentFragment = idx;

        fragmentChangeActions();
    }

    public void fragmentChangeActions() {
        if (iCurrentFragment == 0) {
            ibNearQuestions.setVisibility(View.VISIBLE);
        } else {
            ibNearQuestions.setVisibility(View.GONE);
        }

        if (iCurrentFragment == 4) {
            ibLogout.setVisibility(View.VISIBLE);
            ibSettings.setVisibility(View.VISIBLE);
            ibChat.setVisibility(View.VISIBLE);
            ibFav.setVisibility(View.VISIBLE);
        } else {
            ibLogout.setVisibility(View.GONE);
            ibSettings.setVisibility(View.GONE);
            ibChat.setVisibility(View.GONE);
            ibFav.setVisibility(View.GONE);
        }

        if (iCurrentFragment != 2) {
            if (fPostQuestion.famPost != null) {
                if (fPostQuestion.famPost.isOpen())
                    fPostQuestion.famPost.close(false);
            }
        }
    }

    public void doLogout() {
        bLogoutProcess = true;

        if (tmNotifications != null) {
            tmNotifications.cancel();
            tmNotifications.purge();
            tmNotifications = null;
        }

        ImageLoader.getInstance().clearMemoryCache();
        ImageLoader.getInstance().clearDiskCache();
        ImageLoader.getInstance().destroy();

        QQData.clearPostRequests();
        QQData.clearAllActivities();
        QQData.resetUserData();

        if (QQData.m_bFBAccount)
            LoginManager.getInstance().logOut();

        Intent it = new Intent(HomeActivity.this, LoginActivity.class);
        Bundle translate = ActivityOptions.makeCustomAnimation(HomeActivity.this, R.anim.act_slide_in_right_home, R.anim.act_slide_out_right).toBundle();
        startActivity(it, translate);
        finish();
    }

    public void startNotificationsService() {
        if (QQData.m_iUID == -1)
            return;

        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        Bundle b = new Bundle();
        b.putInt("uid", QQData.m_iUID);
        intent.putExtras(b);

        final PendingIntent pIntent = PendingIntent.getBroadcast(this, AlarmReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 10000, 10000, pIntent);
    }

    public void stopNotificationsService() {
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, AlarmReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }

    public void checkParams() {
        Bundle params = getIntent().getExtras();
        if (params == null || params.isEmpty())
            return;

        String navigate = params.getString("navigate");
        if (navigate.equals("user_profile")) {
            int tid = params.getInt("target_uid");

            Bundle b = new Bundle();
            b.putInt("target_uid", tid);
            Intent it = new Intent(QQData.m_actHome, UserProfileActivity.class);
            it.putExtras(b);
            Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
            startActivity(it, translate);
        } else if (navigate.equals("question")) {
            int qid = params.getInt("qid");

            Bundle b = new Bundle();
            b.putInt("qid", qid);
            Intent it = new Intent(QQData.m_actHome, QuestionActivity.class);
            it.putExtras(b);
            Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
            startActivity(it, translate);
        } else if (navigate.equals("messages")) {
            int tid = params.getInt("tid");

            Bundle b = new Bundle();
            b.putInt("tid", tid);
            Intent it = new Intent(QQData.m_actHome, MessagesActivity.class);
            it.putExtras(b);
            Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
            startActivity(it, translate);
        } else if (navigate.equals("pendings")) {
            Intent it = new Intent(QQData.m_actHome, PendingFollowListActivity.class);
            Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
            startActivity(it, translate);
        }

    }

    // XML OnClick fonksiyonlarını fragmentlere yönlendir

    // F_Home

    public void nearQuestionsClick(View v) {
        fHome.f_nearQuestionsClick(v);
    }

    // F_Search

    public void cancelSearch(View v) {
        fSearch.f_cancelSearch(v);
    }

    // F_PostQuestion

    public void addPostOption(View v) {
        fPostQuestion.f_addPostOption(v);
    }

    public void removePostOption(View v) {
        fPostQuestion.f_removePostOption(v);
    }

    public void setQuestionImage(View v) {
        fPostQuestion.f_setQuestionImage(v);
    }

    public void togglePostLocation(View v) {
        fPostQuestion.f_togglePostLocation(v);
    }

    // F_MyProfile

    public void goEditProfile(View v) {
        fMyProfile.f_goEditProfile(v);
    }

    public void showMyProfilePhoto(View v) {
        fMyProfile.f_showMyProfilePhoto(v);
    }

    public void showMyFollowingList(View v) {
        fMyProfile.f_showMyFollowingList(v);
    }

    public void showMyFollowerList(View v) {
        fMyProfile.f_showMyFollowerList(v);
    }

    public void chatClick(View v) {
        fMyProfile.f_chatClick(v);
    }

    public void favClick(View v) {
        fMyProfile.f_favClick(v);
    }

    public void settingsClick(View v) {
        fMyProfile.f_settingsClick(v);
    }

    public void logoutClick(View v) {
        fMyProfile.f_logoutClick(v);
    }

}
