package com.quiqueapp.qq;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

import java.util.HashMap;
import java.util.Map;

public class InboxActivity extends Activity {

    private SwipeRefreshLayout srlInbox;
    private InteractiveScrollView isvInbox;
    private LinearLayout llInboxLoading, llInboxContainer;
    private Map<Integer, InboxItem> itemList = new HashMap<Integer, InboxItem>();

    private int iScrollLoadPage = 0;
    private boolean bScrollLoadProcess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        QQData.m_lActivityList.add(InboxActivity.this);

        srlInbox = (SwipeRefreshLayout) findViewById(R.id.srlInbox);
        isvInbox = (InteractiveScrollView) findViewById(R.id.isvInbox);

        llInboxLoading = (LinearLayout) findViewById(R.id.llInboxLoading);
        llInboxContainer = (LinearLayout) findViewById(R.id.llInboxContainer);

        srlInbox.setProgressBackgroundColorSchemeColor(Color.parseColor("#EEEEEE"));
        srlInbox.setColorSchemeColors(Color.parseColor("#58ACFA"));
        srlInbox.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                clearInbox();
                getChats();
            }
        });

        isvInbox.setOnBottomReachedListener(new InteractiveScrollView.OnBottomReachedListener() {
            @Override
            public void onBottomReached() {
                if (!bScrollLoadProcess) {
                    srlInbox.setRefreshing(true);
                    bScrollLoadProcess = true;
                    iScrollLoadPage++;
                    getChats();
                }
            }
        });

        getChats();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_slide_in_right, R.anim.act_slide_out_right);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(InboxActivity.this);

        updateMessagesCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(InboxActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(InboxActivity.this);
        }
    }

    public void goHome(View v) {
        QQData.clearAllActivities();
    }

    public void clearInbox() {
        iScrollLoadPage = 0;
        itemList.clear();
        llInboxContainer.removeAllViews();
    }

    public void getChats() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("p", String.valueOf(iScrollLoadPage));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sGetInboxAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlInbox.isRefreshing())
                    srlInbox.setRefreshing(false);

                getResult_Inbox(httpResponse);

                bScrollLoadProcess = false;
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlInbox.isRefreshing())
                    srlInbox.setRefreshing(false);

                bScrollLoadProcess = false;
                iScrollLoadPage--;

                QQData.showError(InboxActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_Inbox(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 931) {   // get my inbox
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));

                    if (!jsonRet.getString("list").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("list"));
                        getList(jsonList);
                    } else
                        iScrollLoadPage--;

                    llInboxLoading.setVisibility(View.GONE);
                    llInboxContainer.setVisibility(View.VISIBLE);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            iScrollLoadPage--;
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(InboxActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void getList(JSONArray jsonList) {
        try {

            int userCount = jsonList.length();

            for (int i = 0; i < userCount; i++) {

                JSONObject jsonUser = jsonList.getJSONObject(i);
                int cid = Integer.parseInt(jsonUser.getString("cid"));
                int userID = Integer.parseInt(jsonUser.getString("uid"));

                JSONObject jsonUserInfo = new JSONObject(jsonUser.getString("info"));
                String userNick = jsonUserInfo.getString("nick");
                String userName = jsonUserInfo.getString("name");

                int userBlockState = Integer.parseInt(jsonUser.getString("block_state"));
                String pp = jsonUser.getString("pp");
                int msgcount = Integer.parseInt(jsonUser.getString("msgcount"));

                InboxItem ibi = new InboxItem(cid, userID, userNick, userName, userBlockState, pp, msgcount);
                addUser(ibi);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addUser(final InboxItem ibi) {

        LinearLayout userItem = (LinearLayout) getLayoutInflater().inflate(R.layout.inbox_item, null);

        LinearLayout LL1 = (LinearLayout) userItem.getChildAt(0);
        LinearLayout LLInfo = (LinearLayout) LL1.getChildAt(1);

        final de.hdodenhof.circleimageview.CircleImageView civPhoto = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(0);
        TextView tvName = (TextView) LLInfo.getChildAt(0);
        TextView tvNick = (TextView) LLInfo.getChildAt(1);

        RelativeLayout RL1 = (RelativeLayout) LL1.getChildAt(2);
        final ImageButton ibRemoveChat = (ImageButton) RL1.getChildAt(0);

        if (!ibi.sPPName.equals("-1")) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(ibi.sPPName), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    civPhoto.setImageBitmap(loadedImage);
                }
            });
        }

        tvName.setText(ibi.sName);
        tvNick.setText("@" + ibi.sNick);

        ibRemoveChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeChat(itemList.get(ibi.iUID));
            }
        });

        civPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goMessages(itemList.get(ibi.iUID));
            }
        });

        tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goMessages(itemList.get(ibi.iUID));
            }
        });

        tvNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goMessages(itemList.get(ibi.iUID));
            }
        });

        ibi.cDesign.llItem = userItem;

        itemList.put(ibi.iUID, ibi);
        llInboxContainer.addView(ibi.cDesign.llItem);

    }

    public void removeChat(final InboxItem ibi) {
        QQData.showConfirm(InboxActivity.this, "Sohbeti silmek istiyor musunuz?", new ActionClickListener() {
            @Override
            public void onActionClicked(Snackbar snackbar) {
                sendRemoveChat(ibi);
            }
        });
    }

    public void sendRemoveChat(final InboxItem ibi) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("cid", String.valueOf(ibi.iCID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sRemoveChatAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_RemoveChat(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(InboxActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);

        itemList.remove(ibi.iUID);
        llInboxContainer.removeView(ibi.cDesign.llItem);
    }

    public void getResult_RemoveChat(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 935) {   // chat removed
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(InboxActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void goMessages(final InboxItem ibi) {
        if (ibi.iBlockedMe == 1) {
            QQData.showError(InboxActivity.this, "Bu kullanıcıya mesaj gönderemezsiniz");
            return;
        }

        Bundle b = new Bundle();
        b.putInt("tid", ibi.iUID);

        Intent it = new Intent(InboxActivity.this, MessagesActivity.class);
        it.putExtras(b);
        Bundle translate = ActivityOptions.makeCustomAnimation(InboxActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
        startActivity(it, translate);
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
                QQData.showError(InboxActivity.this, "İnternet bağlantısı kurulamadı");
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
                QQData.showError(InboxActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }
}
