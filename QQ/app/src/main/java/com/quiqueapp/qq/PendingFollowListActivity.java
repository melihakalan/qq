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

public class PendingFollowListActivity extends Activity {

    private SwipeRefreshLayout srlPendingList;
    private InteractiveScrollView isvPendingList;
    private LinearLayout llPendingListLoading, llPendingListContainer;
    private Map<Integer, PendingListItem> itemList = new HashMap<Integer, PendingListItem>();

    private int iScrollLoadPage = 0;
    private boolean bScrollLoadProcess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_follow_list);

        QQData.m_lActivityList.add(PendingFollowListActivity.this);

        srlPendingList = (SwipeRefreshLayout) findViewById(R.id.srlPendingList);
        isvPendingList = (InteractiveScrollView) findViewById(R.id.isvPendingList);

        llPendingListLoading = (LinearLayout) findViewById(R.id.llPendingListLoading);
        llPendingListContainer = (LinearLayout) findViewById(R.id.llPendingListContainer);

        srlPendingList.setProgressBackgroundColorSchemeColor(Color.parseColor("#EEEEEE"));
        srlPendingList.setColorSchemeColors(Color.parseColor("#58ACFA"));
        srlPendingList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                clearPendingList();
                getPendingList();
            }
        });

        isvPendingList.setOnBottomReachedListener(new InteractiveScrollView.OnBottomReachedListener() {
            @Override
            public void onBottomReached() {
                if (!bScrollLoadProcess) {
                    srlPendingList.setRefreshing(true);
                    bScrollLoadProcess = true;
                    iScrollLoadPage++;
                    getPendingList();
                }
            }
        });

        getPendingList();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_slide_in_right, R.anim.act_slide_out_right);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(PendingFollowListActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(PendingFollowListActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(PendingFollowListActivity.this);
        }
    }

    public void goHome(View v) {
        QQData.clearAllActivities();
    }

    public void clearPendingList() {
        iScrollLoadPage = 0;
        itemList.clear();
        llPendingListContainer.removeAllViews();
    }

    public void getPendingList() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("p", String.valueOf(iScrollLoadPage));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sGetPendingListAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlPendingList.isRefreshing())
                    srlPendingList.setRefreshing(false);

                getResult_PendingList(httpResponse);

                bScrollLoadProcess = false;
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlPendingList.isRefreshing())
                    srlPendingList.setRefreshing(false);

                bScrollLoadProcess = false;
                iScrollLoadPage--;

                QQData.showError(PendingFollowListActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_PendingList(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 825) {   // pending list
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));

                    if (!jsonRet.getString("list").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("list"));
                        getList(jsonList);
                    } else
                        iScrollLoadPage--;

                    llPendingListLoading.setVisibility(View.GONE);
                    llPendingListContainer.setVisibility(View.VISIBLE);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            iScrollLoadPage--;
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(PendingFollowListActivity.this, "İnternet bağlantısı kurulamadı");
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
                int userID = Integer.parseInt(jsonUser.getString("uid"));

                JSONObject jsonUserInfo = new JSONObject(jsonUser.getString("info"));
                String userNick = jsonUserInfo.getString("nick");
                String userName = jsonUserInfo.getString("name");

                String pp = jsonUser.getString("pp");

                PendingListItem pli = new PendingListItem(userID, userNick, userName, pp);
                addUser(pli);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addUser(final PendingListItem pli) {

        LinearLayout userItem = (LinearLayout) getLayoutInflater().inflate(R.layout.pending_list_item, null);

        LinearLayout LL1 = (LinearLayout) userItem.getChildAt(0);
        LinearLayout LLInfo = (LinearLayout) LL1.getChildAt(1);

        final de.hdodenhof.circleimageview.CircleImageView civPhoto = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(0);
        TextView tvName = (TextView) LLInfo.getChildAt(0);
        TextView tvNick = (TextView) LLInfo.getChildAt(1);

        RelativeLayout RL1 = (RelativeLayout) LL1.getChildAt(2);
        final ImageButton ibReject = (ImageButton) RL1.getChildAt(0);
        final ImageButton ibAccept = (ImageButton) RL1.getChildAt(1);

        if (!pli.sPPName.equals("-1")) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(pli.sPPName), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    civPhoto.setImageBitmap(loadedImage);
                }
            });
        }

        tvName.setText(pli.sName);
        tvNick.setText("@" + pli.sNick);

        civPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putInt("target_uid", pli.iUID);
                Intent it = new Intent(PendingFollowListActivity.this, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(PendingFollowListActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putInt("target_uid", pli.iUID);
                Intent it = new Intent(PendingFollowListActivity.this, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(PendingFollowListActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putInt("target_uid", pli.iUID);
                Intent it = new Intent(PendingFollowListActivity.this, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(PendingFollowListActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        ibReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rejectUser(pli);
            }
        });

        ibAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptUser(pli);
            }
        });

        pli.cDesign.llItem = userItem;
        itemList.put(pli.iUID, pli);
        llPendingListContainer.addView(pli.cDesign.llItem);

    }

    public void acceptUser(final PendingListItem pli) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("tid", String.valueOf(pli.iUID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sAcceptPendingAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_AcceptUser(httpResponse, pli);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(PendingFollowListActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);

        itemList.remove(pli.iUID);
        llPendingListContainer.removeView(pli.cDesign.llItem);

        QQData.m_actHome.fNotifications.setPending(--QQData.m_actHome.fNotifications.iPendingCount);
        if (QQData.m_actHome.fNotifications.iPendingCount == 0)
            finish();
    }

    public void getResult_AcceptUser(HttpResponse httpResponse, final PendingListItem fli) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 826) {   // accept pending
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(PendingFollowListActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void rejectUser(final PendingListItem pli) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("tid", String.valueOf(pli.iUID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sRejectPendingAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_RejectUser(httpResponse, pli);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(PendingFollowListActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);

        itemList.remove(pli.iUID);
        llPendingListContainer.removeView(pli.cDesign.llItem);

        QQData.m_actHome.fNotifications.setPending(--QQData.m_actHome.fNotifications.iPendingCount);
        if (QQData.m_actHome.fNotifications.iPendingCount == 0)
            finish();
    }

    public void getResult_RejectUser(HttpResponse httpResponse, final PendingListItem fli) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 827) {   // reject pending
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(PendingFollowListActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }
}
