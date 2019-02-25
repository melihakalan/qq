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

public class FavListActivity extends Activity {

    private int iQID;
    private SwipeRefreshLayout srlFavList;
    private InteractiveScrollView isvFavList;
    private LinearLayout llFavListLoading, llFavListContainer;
    private Map<Integer, FavListItem> itemList = new HashMap<Integer, FavListItem>();

    private int iScrollLoadPage = 0;
    private boolean bScrollLoadProcess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_list);

        QQData.m_lActivityList.add(FavListActivity.this);

        srlFavList = (SwipeRefreshLayout) findViewById(R.id.srlFavList);
        isvFavList = (InteractiveScrollView) findViewById(R.id.isvFavList);

        llFavListLoading = (LinearLayout) findViewById(R.id.llFavListLoading);
        llFavListContainer = (LinearLayout) findViewById(R.id.llFavListContainer);

        srlFavList.setProgressBackgroundColorSchemeColor(Color.parseColor("#EEEEEE"));
        srlFavList.setColorSchemeColors(Color.parseColor("#58ACFA"));
        srlFavList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                clearFavList();
                getFavList();
            }
        });

        isvFavList.setOnBottomReachedListener(new InteractiveScrollView.OnBottomReachedListener() {
            @Override
            public void onBottomReached() {
                if (!bScrollLoadProcess) {
                    srlFavList.setRefreshing(true);
                    bScrollLoadProcess = true;
                    iScrollLoadPage++;
                    getFavList();
                }
            }
        });

        getQuestionInfo();
        getFavList();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_slide_in_right, R.anim.act_slide_out_right);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(FavListActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(FavListActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(FavListActivity.this);
        }
    }

    public void goHome(View v) {
        QQData.clearAllActivities();
    }

    public void getQuestionInfo() {
        Bundle b = getIntent().getExtras();
        iQID = b.getInt("qid");
    }

    public void clearFavList() {
        iScrollLoadPage = 0;
        itemList.clear();
        llFavListContainer.removeAllViews();
    }

    public void getFavList() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(iQID))
                .add("p", String.valueOf(iScrollLoadPage));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sGetFavListAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlFavList.isRefreshing())
                    srlFavList.setRefreshing(false);

                getResult_FavList(httpResponse);

                bScrollLoadProcess = false;
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlFavList.isRefreshing())
                    srlFavList.setRefreshing(false);

                bScrollLoadProcess = false;
                iScrollLoadPage--;

                QQData.showError(FavListActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_FavList(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 631) {   // fav list
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));

                    if (!jsonRet.getString("list").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("list"));
                        getList(jsonList);
                    } else
                        iScrollLoadPage--;

                    llFavListLoading.setVisibility(View.GONE);
                    llFavListContainer.setVisibility(View.VISIBLE);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            iScrollLoadPage--;
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(FavListActivity.this, "İnternet bağlantısı kurulamadı");
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

                int userBlockState = Integer.parseInt(jsonUser.getString("block_state"));
                int userFollowState = Integer.parseInt(jsonUser.getString("follow_state"));
                String pp = jsonUser.getString("pp");

                FavListItem fli = new FavListItem(userID, userNick, userName, userBlockState, userFollowState, pp);
                addUser(fli);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addUser(final FavListItem fli) {

        LinearLayout userItem = (LinearLayout) getLayoutInflater().inflate(R.layout.fav_list_item, null);

        LinearLayout LL1 = (LinearLayout) userItem.getChildAt(0);
        LinearLayout LLInfo = (LinearLayout) LL1.getChildAt(1);

        final de.hdodenhof.circleimageview.CircleImageView civPhoto = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(0);
        TextView tvName = (TextView) LLInfo.getChildAt(0);
        TextView tvNick = (TextView) LLInfo.getChildAt(1);

        RelativeLayout RL1 = (RelativeLayout) LL1.getChildAt(2);
        final ImageButton ibFollowProcess = (ImageButton) RL1.getChildAt(0);

        if (!fli.sPPName.equals("-1")) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(fli.sPPName), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    civPhoto.setImageBitmap(loadedImage);
                }
            });
        }

        tvName.setText(fli.sName);
        tvNick.setText("@" + fli.sNick);

        if (fli.iFollowState == 1) {
            ibFollowProcess.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_unfollow_selector));
        } else if (fli.iFollowState == 2) {
            ibFollowProcess.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_follow_pending_selector));
        }

        if (fli.iBlockedMe == 1 || fli.iUID == QQData.m_iUID) {
            ibFollowProcess.setVisibility(View.INVISIBLE);
        } else {
            ibFollowProcess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    followProcess(itemList.get(fli.iUID));
                }
            });
        }

        civPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (fli.iUID == QQData.m_iUID) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.setFragment(4);
                    return;
                }

                Bundle b = new Bundle();
                b.putInt("target_uid", fli.iUID);
                Intent it = new Intent(FavListActivity.this, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(FavListActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (fli.iUID == QQData.m_iUID) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.setFragment(4);
                    return;
                }

                Bundle b = new Bundle();
                b.putInt("target_uid", fli.iUID);
                Intent it = new Intent(FavListActivity.this, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(FavListActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (fli.iUID == QQData.m_iUID) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.setFragment(4);
                    return;
                }

                Bundle b = new Bundle();
                b.putInt("target_uid", fli.iUID);
                Intent it = new Intent(FavListActivity.this, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(FavListActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        fli.cDesign.llItem = userItem;
        fli.cDesign.ibFollowProcess = ibFollowProcess;

        itemList.put(fli.iUID, fli);
        llFavListContainer.addView(fli.cDesign.llItem);

    }

    public void followProcess(final FavListItem fli) {

        if (fli.iFollowState == 0)
            goFollowUser(fli);
        else if (fli.iFollowState == 1)
            goUnfollowUser(fli);
        else
            goCancelPending(fli);

    }

    public void goFollowUser(final FavListItem fli) {
        QQData.bangEffect(fli.cDesign.ibFollowProcess, FavListActivity.this, Color.parseColor("#3498db"), Color.parseColor("#3498db"), null);

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("target_uid", String.valueOf(fli.iUID))
                .add("op", "1");

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sFollowProcessAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_FollowUser(httpResponse, fli);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(FavListActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_FollowUser(HttpResponse httpResponse, final FavListItem fli) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 231) {   // follow process success
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    int state = Integer.parseInt(jsonRet.getString("state"));

                    if (state == 1) {    //followed
                        if (fli.iFollowState == 0) {
                            fli.iFollowState = 1;
                            fli.cDesign.ibFollowProcess.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_unfollow_selector));
                            QQData.MyInfo.iFollowingCount++;
                            QQData.m_actHome.fMyProfile.f_updateProfileViews();
                        }
                    } else if (state == 2) { //pending
                        fli.iFollowState = 2;
                        fli.cDesign.ibFollowProcess.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_follow_pending_selector));
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(FavListActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void goCancelPending(final FavListItem fli) {
        QQData.bangEffect(fli.cDesign.ibFollowProcess, FavListActivity.this, Color.parseColor("#666666"), Color.parseColor("#666666"), null);

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("target_uid", String.valueOf(fli.iUID))
                .add("op", "2");

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sFollowProcessAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_CancelPending(httpResponse, fli);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(FavListActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_CancelPending(HttpResponse httpResponse, final FavListItem fli) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 231) {   // follow process success
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    fli.iFollowState = 0;
                    fli.cDesign.ibFollowProcess.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_follow_selector));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(FavListActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void goUnfollowUser(final FavListItem fli) {
        QQData.bangEffect(fli.cDesign.ibFollowProcess, FavListActivity.this, Color.parseColor("#2ecc71"), Color.parseColor("#2ecc71"), null);

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("target_uid", String.valueOf(fli.iUID))
                .add("op", "0");

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sFollowProcessAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_UnfollowUser(httpResponse, fli);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(FavListActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_UnfollowUser(HttpResponse httpResponse, final FavListItem fli) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 231) {   // follow process success
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    if (fli.iFollowState == 1) {
                        fli.iFollowState = 0;
                        fli.cDesign.ibFollowProcess.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_follow_selector));
                        QQData.MyInfo.iFollowingCount--;
                        QQData.m_actHome.fMyProfile.f_updateProfileViews();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(FavListActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }
}
