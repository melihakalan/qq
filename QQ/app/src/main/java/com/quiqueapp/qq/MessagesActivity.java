package com.quiqueapp.qq;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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

public class MessagesActivity extends Activity {

    private int iTargetUID;
    private String sNick, sName, sPP;
    private SwipeRefreshLayout srlMessages;
    private InteractiveScrollView isvMessages;
    private LinearLayout llMessagesLoading, llMessagesContainer;

    private RelativeLayout rlLoadMore;
    private EditText edMessage;

    private int iScrollLoadPage = 0;
    private boolean bScrollLoadProcess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        QQData.m_lActivityList.add(MessagesActivity.this);

        srlMessages = (SwipeRefreshLayout) findViewById(R.id.srlMessages);
        isvMessages = (InteractiveScrollView) findViewById(R.id.isvMessages);

        llMessagesLoading = (LinearLayout) findViewById(R.id.llMessagesLoading);
        llMessagesContainer = (LinearLayout) findViewById(R.id.llMessagesContainer);

        srlMessages.setProgressBackgroundColorSchemeColor(Color.parseColor("#EEEEEE"));
        srlMessages.setColorSchemeColors(Color.parseColor("#58ACFA"));
        srlMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                clearMessages();
                getMessages();
            }
        });

        rlLoadMore = (RelativeLayout) findViewById(R.id.rlLoadMore);
        edMessage = (EditText) findViewById(R.id.edMessage);

        getTargetInfo();
        getMessagerInfo();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_slide_in_right, R.anim.act_slide_out_right);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(MessagesActivity.this);

        updateMessagesCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(MessagesActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(MessagesActivity.this);
        }
    }

    public void goHome(View v) {
        QQData.clearAllActivities();
    }

    public void getTargetInfo() {
        Bundle b = getIntent().getExtras();
        iTargetUID = b.getInt("tid");
    }

    public void clearMessages() {
        iScrollLoadPage = 0;
        llMessagesContainer.removeAllViews();
        rlLoadMore.setVisibility(View.GONE);
    }

    public void getMessagerInfo() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("tid", String.valueOf(iTargetUID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sGetMessagerInfoAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_MessagerInfo(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(MessagesActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_MessagerInfo(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 941) {   // get messager info
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));

                    sNick = jsonRet.getString("nick");
                    sName = jsonRet.getString("name");
                    sPP = jsonRet.getString("pp");

                    getMessages();

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(MessagesActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void getMessages() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("tid", String.valueOf(iTargetUID))
                .add("p", String.valueOf(iScrollLoadPage));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sGetMessagesAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlMessages.isRefreshing())
                    srlMessages.setRefreshing(false);

                getResult_Messages(httpResponse);

                bScrollLoadProcess = false;
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlMessages.isRefreshing())
                    srlMessages.setRefreshing(false);

                bScrollLoadProcess = false;
                iScrollLoadPage--;

                QQData.showError(MessagesActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_Messages(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 945) {   // get messages
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));

                    if (!jsonRet.getString("list").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("list"));
                        getList(jsonList);

                        if (iScrollLoadPage == 0) {
                            isvMessages.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    isvMessages.fullScroll(ScrollView.FOCUS_DOWN);
                                }
                            }, 1000);
                        }

                        if (llMessagesContainer.getChildCount() >= 20)
                            rlLoadMore.setVisibility(View.VISIBLE);
                    } else
                        iScrollLoadPage--;

                    llMessagesLoading.setVisibility(View.GONE);
                    llMessagesContainer.setVisibility(View.VISIBLE);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            iScrollLoadPage--;
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(MessagesActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void getList(JSONArray jsonList) {
        try {

            int msgCount = jsonList.length();

            for (int i = 0; i < msgCount; i++) {

                JSONObject jsonMsg = jsonList.getJSONObject(i);
                int uid = Integer.parseInt(jsonMsg.getString("uid"));
                int tid = Integer.parseInt(jsonMsg.getString("tid"));
                String msg = jsonMsg.getString("msg");
                String date = jsonMsg.getString("date");

                if (uid == QQData.m_iUID)
                    addMessageMy(msg, false, date);
                else
                    addMessageOther(msg, date);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(View v) {

        String msg = edMessage.getText().toString();

        if (msg.length() == 0)
            return;

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("tid", String.valueOf(iTargetUID))
                .add("msg", msg);

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSendMessageAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_SendMessage(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(MessagesActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
        addMessageMy(msg, true, null);
        edMessage.setText("");

        isvMessages.postDelayed(new Runnable() {
            @Override
            public void run() {
                isvMessages.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 500);
    }

    public void getResult_SendMessage(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 951) {   // message sent
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                } else if (iretval == 950) {   //chat deleted or blocked
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(MessagesActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void addMessageMy(String msg, boolean sending, String date) {

        LinearLayout msgItem = (LinearLayout) getLayoutInflater().inflate(R.layout.msg_item_1, null);

        LinearLayout LL1 = (LinearLayout) msgItem.getChildAt(0);
        final de.hdodenhof.circleimageview.CircleImageView civPhoto = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(0);

        LinearLayout LL2 = (LinearLayout) LL1.getChildAt(1);
        TextView tvNick = (TextView) LL2.getChildAt(0);
        TextView tvMsg = (TextView) LL2.getChildAt(1);

        if (QQData.MyInfo.sPPName != null) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(QQData.MyInfo.sPPName), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    civPhoto.setImageBitmap(loadedImage);
                }
            });
        }

        civPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QQData.clearAllActivities();
                QQData.m_actHome.setFragment(4);
            }
        });

        tvMsg.setText(msg);
        if (sending)
            tvNick.setText("@" + QQData.MyInfo.sNick + " | şimdi");
        else
            tvNick.setText("@" + QQData.MyInfo.sNick + " | " + QQData.getDateDiff(date));

        if (sending)
            llMessagesContainer.addView(msgItem);
        else {
            if (iScrollLoadPage == 0)
                llMessagesContainer.addView(msgItem);
            else {
                llMessagesContainer.addView(msgItem, 0);
            }
        }

    }

    public void addMessageOther(String msg, String date) {

        LinearLayout msgItem = (LinearLayout) getLayoutInflater().inflate(R.layout.msg_item_2, null);

        LinearLayout LL1 = (LinearLayout) msgItem.getChildAt(0);
        final de.hdodenhof.circleimageview.CircleImageView civPhoto = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(1);

        LinearLayout LL2 = (LinearLayout) LL1.getChildAt(0);
        TextView tvNick = (TextView) LL2.getChildAt(0);
        TextView tvMsg = (TextView) LL2.getChildAt(1);

        if (!sPP.equals("-1")) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(sPP), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    civPhoto.setImageBitmap(loadedImage);
                }
            });
        }

        civPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putInt("target_uid", iTargetUID);
                Intent it = new Intent(MessagesActivity.this, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(MessagesActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvNick.setText(QQData.getDateDiff(date) + " | @" + sNick);
        tvMsg.setText(msg);

        if (iScrollLoadPage == 0)
            llMessagesContainer.addView(msgItem);
        else {
            llMessagesContainer.addView(msgItem, 0);
        }
    }

    public void loadMore(View v) {
        if (!bScrollLoadProcess) {
            srlMessages.setRefreshing(true);
            bScrollLoadProcess = true;
            iScrollLoadPage++;
            getMessages();
        }
    }

    public void updateMessagesCheck() {
        QQData.m_actHome.ibChat.setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_myprofile_chat_selector));

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sUpdateMessagesCheckAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_UpdateMessagesCheck(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(MessagesActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_UpdateMessagesCheck(HttpResponse httpResponse) {
        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 802) {   // update
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(MessagesActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }
}
