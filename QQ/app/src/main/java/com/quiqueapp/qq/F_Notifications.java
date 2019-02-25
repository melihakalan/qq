package com.quiqueapp.qq;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.core.imageaware.NonViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by user on 17.11.2015.
 */
public class F_Notifications extends Fragment {

    private SwipeRefreshLayout srlNotifications;
    private LinearLayout llNotificationsLoading, llNotificationsContainer, llNotificationsTop, llNotificationsContents;
    private View vNotificationsLine;

    public int iPendingCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.f_notifications, container, false);

        llNotificationsLoading = (LinearLayout) v.findViewById(R.id.llNotificationsLoading);
        llNotificationsContainer = (LinearLayout) v.findViewById(R.id.llNotificationsContainer);
        llNotificationsTop = (LinearLayout) v.findViewById(R.id.llNotificationsTop);
        llNotificationsContents = (LinearLayout) v.findViewById(R.id.llNotificationsContents);
        vNotificationsLine = v.findViewById(R.id.vNotificationsLine);
        srlNotifications = (SwipeRefreshLayout) v.findViewById(R.id.srlNotifications);

        srlNotifications.setProgressBackgroundColorSchemeColor(Color.parseColor("#EEEEEE"));
        srlNotifications.setColorSchemeColors(Color.parseColor("#58ACFA"));
        srlNotifications.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                clearNotifications();
                getNotifications();
                updateNotifications();
            }
        });

        getNotifications();

        return v;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden)
            updateNotifications();
    }

    public void clearNotifications() {
        llNotificationsContents.removeAllViews();
        llNotificationsTop.setVisibility(View.GONE);
        vNotificationsLine.setVisibility(View.GONE);
    }

    public void getNotifications() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sGetNotificationsAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlNotifications.isRefreshing())
                    srlNotifications.setRefreshing(false);

                getResult_GetNotifications(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlNotifications.isRefreshing())
                    srlNotifications.setRefreshing(false);

                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_GetNotifications(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 810) {   // notifications
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));

                    if (!jsonRet.getString("list").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("list"));
                        getList(jsonList);
                    }

                    int pending_count = Integer.parseInt(jsonRet.getString("pendingcount"));
                    setPending(pending_count);

                    int newmsg = Integer.parseInt(jsonRet.getString("newmsg"));
                    setNewMessage(newmsg);

                    llNotificationsLoading.setVisibility(View.GONE);
                    llNotificationsContainer.setVisibility(View.VISIBLE);
                    llNotificationsContents.setVisibility(View.VISIBLE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void getList(JSONArray jsonList) {
        try {

            boolean hasnew = false;
            int count = jsonList.length();

            for (int i = 0; i < count; i++) {

                JSONObject jsonNotification = jsonList.getJSONObject(i);

                int uid = Integer.parseInt(jsonNotification.getString("uid"));
                int uid2 = Integer.parseInt(jsonNotification.getString("uid2"));
                int type = Integer.parseInt(jsonNotification.getString("type"));
                int param = -1;
                if (!jsonNotification.getString("param").equals("null"))
                    param = Integer.parseInt(jsonNotification.getString("param"));
                String date = jsonNotification.getString("date");
                int isnew = Integer.parseInt(jsonNotification.getString("new"));
                String name = jsonNotification.getString("name");
                String pp = jsonNotification.getString("pp");

                addNotification(uid2, type, param, date, isnew, name, pp);

                if (isnew == 1)
                    hasnew = true;
            }

            if (hasnew)
                QQData.m_actHome.ibToolbarSet[3].setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_toolbar_notifications_active));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addNotification(final int uid, int type, final int param, String date, int isnew, String name, String pp) {
        LinearLayout nItem = (LinearLayout) QQData.m_actHome.getLayoutInflater().inflate(R.layout.notification_item, null);
        LinearLayout LL1 = (LinearLayout) nItem.getChildAt(0);
        final CircleImageView civPhoto = (CircleImageView) LL1.getChildAt(0);

        LinearLayout LL2 = (LinearLayout) LL1.getChildAt(1);
        TextView tvName = (TextView) LL2.getChildAt(0);

        LinearLayout LL3 = (LinearLayout) LL2.getChildAt(1);
        TextView tvNotification = (TextView) LL3.getChildAt(0);
        TextView tvDate = (TextView) LL3.getChildAt(1);

        if (!pp.equals("-1")) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(pp), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    civPhoto.setImageBitmap(loadedImage);
                }
            });
        }

        tvName.setText(name);
        tvDate.setText("| " + QQData.getDateDiff(date));
        civPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putInt("target_uid", uid);
                Intent it = new Intent(QQData.m_actHome, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putInt("target_uid", uid);
                Intent it = new Intent(QQData.m_actHome, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        switch (type) {
            case 10:
                tvNotification.setText("seni takip etmeye başladı");
                break;

            case 13:
                tvNotification.setText("takip isteğini kabul etti");
                break;

            case 20:
                tvNotification.setText("gönderini beğendi");
                tvNotification.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle b = new Bundle();
                        b.putInt("qid", param);
                        Intent it = new Intent(QQData.m_actHome, QuestionActivity.class);
                        it.putExtras(b);
                        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                        startActivity(it, translate);
                    }
                });
                break;

            case 21:
                tvNotification.setText("gönderine bir yorum yaptı");
                tvNotification.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle b = new Bundle();
                        b.putInt("qid", param);
                        Intent it = new Intent(QQData.m_actHome, QuestionActivity.class);
                        it.putExtras(b);
                        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                        startActivity(it, translate);
                    }
                });
                break;

            case 22:
                tvNotification.setText("gönderini tekrar paylaştı");
                tvNotification.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle b = new Bundle();
                        b.putInt("qid", param);
                        Intent it = new Intent(QQData.m_actHome, QuestionActivity.class);
                        it.putExtras(b);
                        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                        startActivity(it, translate);
                    }
                });
                break;

            case 23:
                tvNotification.setText("bir gönderide senden bahsetti");
                tvNotification.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle b = new Bundle();
                        b.putInt("qid", param);
                        Intent it = new Intent(QQData.m_actHome, QuestionActivity.class);
                        it.putExtras(b);
                        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                        startActivity(it, translate);
                    }
                });
                break;
        }

        if (isnew == 1) {
            LL1.setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.bg_notification_new_item));
        }

        llNotificationsContents.addView(nItem);
    }

    public void setPending(int count) {
        llNotificationsTop.removeAllViews();

        if (count <= 0) {
            llNotificationsTop.setVisibility(View.GONE);
            vNotificationsLine.setVisibility(View.GONE);
            iPendingCount = 0;
            return;
        }

        LinearLayout nItem = (LinearLayout) QQData.m_actHome.getLayoutInflater().inflate(R.layout.notification_pending_item, null);
        LinearLayout LL1 = (LinearLayout) nItem.getChildAt(0);

        ImageView ivIcon = (ImageView) LL1.getChildAt(0);
        TextView tvInfo = (TextView) LL1.getChildAt(1);
        TextView tvIcon = (TextView) LL1.getChildAt(2);

        ivIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(QQData.m_actHome, PendingFollowListActivity.class);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(QQData.m_actHome, PendingFollowListActivity.class);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(QQData.m_actHome, PendingFollowListActivity.class);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvInfo.setText("Takip İstekleri (" + count + ")");
        llNotificationsTop.setVisibility(View.VISIBLE);
        vNotificationsLine.setVisibility(View.VISIBLE);
        llNotificationsTop.addView(nItem);

        iPendingCount = count;
    }

    public void updateNotifications() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sUpdateNotificationsAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_UpdateNotifications(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_UpdateNotifications(HttpResponse httpResponse) {
        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 801) {   // update
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void getNewNotifications() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sGetNewNotificationsAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_GetNewNotifications(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_GetNewNotifications(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 820) {   // new notifications
                    if (!jsonRet.getString("list").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("list"));
                        getNewList(jsonList);
                    }

                    int pending_count = Integer.parseInt(jsonRet.getString("pendingcount"));
                    setPending(pending_count);
                    if (pending_count > 0 && !jsonRet.getString("pendinglist").equals("0")) {
                        JSONArray jsonPendingList = new JSONArray(jsonRet.getString("pendinglist"));
                        getNewPendingList(jsonPendingList);
                    }

                    if (!jsonRet.getString("newmessages").equals("0")) {
                        JSONArray jsonNewMessages = new JSONArray(jsonRet.getString("newmessages"));
                        getNewMessages(jsonNewMessages);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            int ret = Integer.parseInt(sRet);
            if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void getNewList(JSONArray jsonList) {
        try {

            int count = jsonList.length();

            for (int i = 0; i < count; i++) {

                JSONObject jsonNotification = jsonList.getJSONObject(i);

                int uid = Integer.parseInt(jsonNotification.getString("uid"));
                int uid2 = Integer.parseInt(jsonNotification.getString("uid2"));
                int type = Integer.parseInt(jsonNotification.getString("type"));
                int param = -1;
                if (!jsonNotification.getString("param").equals("null"))
                    param = Integer.parseInt(jsonNotification.getString("param"));
                String date = jsonNotification.getString("date");
                String name = jsonNotification.getString("name");
                String pp = jsonNotification.getString("pp");

                addNewNotification(uid2, type, param, date, name, pp);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addNewNotification(final int uid, int type, final int param, String date, String name, String pp) {
        if (type == 12) {
            QQData.showInfoButton(QQData.getTopActivity(), name + " yeni bir gönderi paylaştı", "GÖR", new ActionClickListener() {
                @Override
                public void onActionClicked(Snackbar snackbar) {
                    Bundle b = new Bundle();
                    b.putInt("qid", param);
                    Intent it = new Intent(QQData.m_actHome, QuestionActivity.class);
                    it.putExtras(b);
                    Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                    startActivity(it, translate);
                }
            });
            return;
        }

        LinearLayout nItem = (LinearLayout) QQData.m_actHome.getLayoutInflater().inflate(R.layout.notification_item, null);
        LinearLayout LL1 = (LinearLayout) nItem.getChildAt(0);
        final CircleImageView civPhoto = (CircleImageView) LL1.getChildAt(0);

        LinearLayout LL2 = (LinearLayout) LL1.getChildAt(1);
        TextView tvName = (TextView) LL2.getChildAt(0);

        LinearLayout LL3 = (LinearLayout) LL2.getChildAt(1);
        TextView tvNotification = (TextView) LL3.getChildAt(0);
        TextView tvDate = (TextView) LL3.getChildAt(1);

        if (!pp.equals("-1")) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(pp), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    civPhoto.setImageBitmap(loadedImage);
                }
            });
        }

        tvName.setText(name);
        tvDate.setText("| " + QQData.getDateDiff(date));
        civPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putInt("target_uid", uid);
                Intent it = new Intent(QQData.m_actHome, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putInt("target_uid", uid);
                Intent it = new Intent(QQData.m_actHome, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        switch (type) {
            case 10:
                tvNotification.setText("seni takip etmeye başladı");
                break;

            case 13:
                tvNotification.setText("takip isteğini kabul etti");
                break;

            case 20:
                tvNotification.setText("gönderini beğendi");
                tvNotification.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle b = new Bundle();
                        b.putInt("qid", param);
                        Intent it = new Intent(QQData.m_actHome, QuestionActivity.class);
                        it.putExtras(b);
                        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                        startActivity(it, translate);
                    }
                });
                break;

            case 21:
                tvNotification.setText("gönderine bir yorum yaptı");
                tvNotification.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle b = new Bundle();
                        b.putInt("qid", param);
                        Intent it = new Intent(QQData.m_actHome, QuestionActivity.class);
                        it.putExtras(b);
                        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                        startActivity(it, translate);
                    }
                });
                break;

            case 22:
                tvNotification.setText("gönderini tekrar paylaştı");
                tvNotification.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle b = new Bundle();
                        b.putInt("qid", param);
                        Intent it = new Intent(QQData.m_actHome, QuestionActivity.class);
                        it.putExtras(b);
                        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                        startActivity(it, translate);
                    }
                });
                break;

            case 23:
                tvNotification.setText("bir gönderide senden bahsetti");
                tvNotification.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle b = new Bundle();
                        b.putInt("qid", param);
                        Intent it = new Intent(QQData.m_actHome, QuestionActivity.class);
                        it.putExtras(b);
                        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                        startActivity(it, translate);
                    }
                });
                break;
        }

        LL1.setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.bg_notification_new_item));
        llNotificationsContents.addView(nItem, 0);
        QQData.m_actHome.ibToolbarSet[3].setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_toolbar_notifications_active));
    }

    public void getNewPendingList(JSONArray jsonPendingList) {
        try {

            int count = jsonPendingList.length();

            for (int i = 0; i < count; i++) {

                JSONObject jsonPending = jsonPendingList.getJSONObject(i);
                String uid = jsonPending.getString("uid");
                String date = jsonPending.getString("date");
                String name = jsonPending.getString("name");

                QQData.showInfoButton(QQData.getTopActivity(), name + " seni takip etmek istiyor", "GÖR", new ActionClickListener() {
                    @Override
                    public void onActionClicked(Snackbar snackbar) {
                        Intent it = new Intent(QQData.m_actHome, PendingFollowListActivity.class);
                        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                        startActivity(it, translate);
                    }
                });
            }
            QQData.m_actHome.ibToolbarSet[3].setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_toolbar_notifications_active));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setNewMessage(int newmsg) {
        if (newmsg == 1) {
            QQData.m_actHome.ibChat.setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_chat_topbar_active));
            QQData.m_actHome.ibToolbarSet[4].setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_toolbar_myprofile_active));
        }
    }

    public void getNewMessages(JSONArray jsonNewMessages) {
        if (!QQData.m_lVisibleActivityList.isEmpty()) {
            if (QQData.m_lVisibleActivityList.get(0) instanceof InboxActivity || QQData.m_lVisibleActivityList.get(0) instanceof MessagesActivity)
                return;
        }

        try {

            int count = jsonNewMessages.length();

            for (int i = 0; i < count; i++) {

                JSONObject jsonMessage = jsonNewMessages.getJSONObject(i);
                final int uid = Integer.parseInt(jsonMessage.getString("uid"));
                String date = jsonMessage.getString("date");
                String name = jsonMessage.getString("name");

                QQData.showInfoButton(QQData.getTopActivity(), name + " sana bir mesaj gönderdi", "GÖR", new ActionClickListener() {
                    @Override
                    public void onActionClicked(Snackbar snackbar) {
                        Bundle b = new Bundle();
                        b.putInt("tid", uid);
                        Intent it = new Intent(QQData.m_actHome, MessagesActivity.class);
                        it.putExtras(b);
                        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                        startActivity(it, translate);
                    }
                });

                setNewMessage(1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}