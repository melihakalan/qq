package com.quiqueapp.qq;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import fr.castorflex.android.circularprogressbar.CircularProgressBar;

/**
 * Created by user on 17.11.2015.
 */
public class F_Search extends Fragment {

    private ImageView ivSearchIcon;
    private ImageButton ibCancelSearch;
    private EditText edSearch;
    private LinearLayout llSearchResults, llTrendingTagsContainer, llTrendingTags;
    private SwipeRefreshLayout srlSearch;
    private InteractiveScrollView isvSearch;

    private FrameLayout flSearchStatus;
    private CircularProgressBar cpbSearch;
    private TextView tvNoSearchResults;

    private boolean bOnSearch = false;
    private int iScrollLoadPage = 0;
    private boolean bScrollLoadProcess = false;

    private int iSearchType = 1;
    private String sSearchQuery = null;

    private CountDownTimer cdt = new CountDownTimer(1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            switch (iSearchType) {
                case 1:
                    searchTag(sSearchQuery);
                    break;
                case 2:
                    searchNick(sSearchQuery);
                    break;
                case 3:
                    searchName(sSearchQuery);
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.f_search, container, false);

        ivSearchIcon = (ImageView) v.findViewById(R.id.ivSearchIcon);
        ibCancelSearch = (ImageButton) v.findViewById(R.id.ibCancelSearch);
        edSearch = (EditText) v.findViewById(R.id.edSearch);
        llSearchResults = (LinearLayout) v.findViewById(R.id.llSearchResults);
        llTrendingTagsContainer = (LinearLayout) v.findViewById(R.id.llTrendingTagsContainer);
        llTrendingTags = (LinearLayout) v.findViewById(R.id.llTrendingTags);
        srlSearch = (SwipeRefreshLayout) v.findViewById(R.id.srlSearch);
        isvSearch = (InteractiveScrollView) v.findViewById(R.id.isvSearch);

        flSearchStatus = (FrameLayout) v.findViewById(R.id.flSearchStatus);
        cpbSearch = (CircularProgressBar) v.findViewById(R.id.cpbSearch);
        tvNoSearchResults = (TextView) v.findViewById(R.id.tvNoSearchResults);

        edSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                sendSearch(s.toString());
            }
        });

        srlSearch.setProgressBackgroundColorSchemeColor(Color.parseColor("#EEEEEE"));
        srlSearch.setColorSchemeColors(Color.parseColor("#58ACFA"));
        srlSearch.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!bOnSearch) {
                    clearTags();
                    getTrendingTags();
                } else
                    srlSearch.setRefreshing(false);
            }
        });

        isvSearch.setOnBottomReachedListener(new InteractiveScrollView.OnBottomReachedListener() {
            @Override
            public void onBottomReached() {
                if (bOnSearch && !bScrollLoadProcess) {
                    srlSearch.setRefreshing(true);
                    bScrollLoadProcess = true;
                    iScrollLoadPage++;
                }
            }
        });

        getTrendingTags();

        return v;
    }

    public void clearTags() {
        llTrendingTags.removeAllViews();
    }

    public void getTrendingTags() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sGetTrendingTagsAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlSearch.isRefreshing())
                    srlSearch.setRefreshing(false);

                getResult_TrendingTags(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlSearch.isRefreshing())
                    srlSearch.setRefreshing(false);


                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_TrendingTags(HttpResponse httpResponse) {
        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 701) {   // get trending tags
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));

                    if (!jsonRet.getString("tags").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("tags"));
                        getTagList(jsonList);
                    }
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

    public void getTagList(JSONArray jsonList) {
        try {
            int count = jsonList.length();

            for (int i = 0; i < count; i++) {
                JSONObject jsonTag = jsonList.getJSONObject(i);
                String tag = jsonTag.getString("tag");
                addTrendingTag(tag);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addTrendingTag(final String tag) {
        TextView tvTag = (TextView) QQData.m_actHome.getLayoutInflater().inflate(R.layout.trendingtag_textview, null);
        tvTag.setText(tag);

        tvTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putString("tag", tag);
                Intent it = new Intent(QQData.m_actHome, TagSearchActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        llTrendingTags.addView(tvTag);
    }

    public void f_cancelSearch(View v) {
        edSearch.setText("");
    }

    public void sendSearch(String s) {
        if (s.trim().length() == 0) {
            bOnSearch = false;
            llSearchResults.removeAllViews();
            iScrollLoadPage = 0;
            ibCancelSearch.setVisibility(View.GONE);
            ivSearchIcon.setVisibility(View.VISIBLE);
            llSearchResults.setVisibility(View.GONE);
            llTrendingTagsContainer.setVisibility(View.VISIBLE);
            flSearchStatus.setVisibility(View.GONE);
            return;
        } else {
            if (llSearchResults.getVisibility() != View.VISIBLE) {
                bOnSearch = true;
                llSearchResults.removeAllViews();
                iScrollLoadPage = 0;
                ivSearchIcon.setVisibility(View.GONE);
                ibCancelSearch.setVisibility(View.VISIBLE);
                llTrendingTagsContainer.setVisibility(View.GONE);
                llSearchResults.setVisibility(View.VISIBLE);
                flSearchStatus.setVisibility(View.VISIBLE);
            }
        }

        if (s.trim().length() == 1 && (s.equals("#") || s.equals("@"))) {
            cpbSearch.setVisibility(View.GONE);
            tvNoSearchResults.setVisibility(View.GONE);
            return;
        }

        llSearchResults.removeAllViews();
        iScrollLoadPage = 0;

        if (s.charAt(0) == '#') {
            cdt.cancel();
            iSearchType = 1;
            sSearchQuery = s;
            cdt.start();
            cpbSearch.setVisibility(View.VISIBLE);
            tvNoSearchResults.setVisibility(View.GONE);
        } else if (s.charAt(0) == '@') {
            String nick = s.substring(1);
            cdt.cancel();
            iSearchType = 2;
            sSearchQuery = nick;
            cdt.start();
            cpbSearch.setVisibility(View.VISIBLE);
            tvNoSearchResults.setVisibility(View.GONE);
        } else {
            cdt.cancel();
            iSearchType = 3;
            sSearchQuery = s;
            cdt.start();
            cpbSearch.setVisibility(View.VISIBLE);
            tvNoSearchResults.setVisibility(View.GONE);
        }
    }

    public void searchTag(String tag) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("tag", tag)
                .add("p", String.valueOf(iScrollLoadPage));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSearchTagAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlSearch.isRefreshing())
                    srlSearch.setRefreshing(false);

                getResult_TagList(httpResponse);

                bScrollLoadProcess = false;
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlSearch.isRefreshing())
                    srlSearch.setRefreshing(false);

                bScrollLoadProcess = false;
                iScrollLoadPage--;

                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_TagList(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 721) {   // tag list
                    llSearchResults.removeAllViews();

                    if (!jsonRet.getString("tags").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("tags"));
                        getSearchTagList(jsonList);
                        cpbSearch.setVisibility(View.GONE);
                    } else {
                        cpbSearch.setVisibility(View.GONE);
                        tvNoSearchResults.setVisibility(View.VISIBLE);
                        iScrollLoadPage--;
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            iScrollLoadPage--;
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            }
        }
    }

    public void getSearchTagList(JSONArray jsonList) {
        try {

            int tagCount = jsonList.length();

            for (int i = 0; i < tagCount; i++) {

                JSONObject jsonTag = jsonList.getJSONObject(i);
                String tag = jsonTag.getString("tag");
                int cnt = Integer.parseInt(jsonTag.getString("cnt"));

                addTag(tag, cnt);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addTag(final String tag, int cnt) {
        LinearLayout tagItem = (LinearLayout) QQData.m_actHome.getLayoutInflater().inflate(R.layout.tag_item, null);
        TextView tvTag = (TextView) tagItem.getChildAt(0);
        TextView tvCnt = (TextView) tagItem.getChildAt(1);

        tvTag.setText(tag);
        tvCnt.setText("| " + cnt + " gönderi");

        tvTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putString("tag", tag);
                Intent it = new Intent(QQData.m_actHome, TagSearchActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvCnt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putString("tag", tag);
                Intent it = new Intent(QQData.m_actHome, TagSearchActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        llSearchResults.addView(tagItem);
    }

    public void searchNick(String nick) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("nick", nick)
                .add("p", String.valueOf(iScrollLoadPage));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSearchNickAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlSearch.isRefreshing())
                    srlSearch.setRefreshing(false);

                getResult_NickList(httpResponse);

                bScrollLoadProcess = false;
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlSearch.isRefreshing())
                    srlSearch.setRefreshing(false);

                bScrollLoadProcess = false;
                iScrollLoadPage--;

                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_NickList(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 731) {   // nick list
                    llSearchResults.removeAllViews();

                    if (!jsonRet.getString("users").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("users"));
                        getNickList(jsonList);
                        cpbSearch.setVisibility(View.GONE);
                    } else {
                        cpbSearch.setVisibility(View.GONE);
                        tvNoSearchResults.setVisibility(View.VISIBLE);
                        iScrollLoadPage--;
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            iScrollLoadPage--;
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            }
        }
    }

    public void getNickList(JSONArray jsonList) {
        try {

            int userCount = jsonList.length();

            for (int i = 0; i < userCount; i++) {

                JSONObject jsonUser = jsonList.getJSONObject(i);
                int userID = Integer.parseInt(jsonUser.getString("uid"));

                JSONObject jsonUserInfo = new JSONObject(jsonUser.getString("info"));
                String userNick = jsonUserInfo.getString("nick");
                String userName = jsonUserInfo.getString("name");

                String pp = jsonUser.getString("pp");

                addNickUser(userID, userNick, userName, pp);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addNickUser(final int uid, String nick, String name, String pp) {

        LinearLayout userItem = (LinearLayout) QQData.m_actHome.getLayoutInflater().inflate(R.layout.user_list_item, null);

        LinearLayout LL1 = (LinearLayout) userItem.getChildAt(0);
        LinearLayout LLInfo = (LinearLayout) LL1.getChildAt(1);

        final de.hdodenhof.circleimageview.CircleImageView civPhoto = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(0);
        TextView tvName = (TextView) LLInfo.getChildAt(0);
        TextView tvNick = (TextView) LLInfo.getChildAt(1);

        if (!pp.equals("-1")) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(pp), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    civPhoto.setImageBitmap(loadedImage);
                }
            });
        }

        tvName.setText(name);
        tvNick.setText("@" + nick);

        civPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (uid == QQData.m_iUID) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.setFragment(4);
                    return;
                }

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

                if (uid == QQData.m_iUID) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.setFragment(4);
                    return;
                }

                Bundle b = new Bundle();
                b.putInt("target_uid", uid);
                Intent it = new Intent(QQData.m_actHome, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (uid == QQData.m_iUID) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.setFragment(4);
                    return;
                }

                Bundle b = new Bundle();
                b.putInt("target_uid", uid);
                Intent it = new Intent(QQData.m_actHome, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });


        llSearchResults.addView(userItem);
    }

    public void searchName(String name) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("name", name)
                .add("p", String.valueOf(iScrollLoadPage));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSearchNameAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlSearch.isRefreshing())
                    srlSearch.setRefreshing(false);

                getResult_NameList(httpResponse);

                bScrollLoadProcess = false;
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlSearch.isRefreshing())
                    srlSearch.setRefreshing(false);

                bScrollLoadProcess = false;
                iScrollLoadPage--;

                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_NameList(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 741) {   // nick list
                    llSearchResults.removeAllViews();

                    if (!jsonRet.getString("users").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("users"));
                        getNameList(jsonList);
                        cpbSearch.setVisibility(View.GONE);
                    } else {
                        cpbSearch.setVisibility(View.GONE);
                        tvNoSearchResults.setVisibility(View.VISIBLE);
                        iScrollLoadPage--;
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            iScrollLoadPage--;
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            }
        }
    }

    public void getNameList(JSONArray jsonList) {
        try {

            int userCount = jsonList.length();

            for (int i = 0; i < userCount; i++) {

                JSONObject jsonUser = jsonList.getJSONObject(i);
                int userID = Integer.parseInt(jsonUser.getString("uid"));

                JSONObject jsonUserInfo = new JSONObject(jsonUser.getString("info"));
                String userNick = jsonUserInfo.getString("nick");
                String userName = jsonUserInfo.getString("name");

                String pp = jsonUser.getString("pp");

                addNameUser(userID, userNick, userName, pp);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addNameUser(final int uid, String nick, String name, String pp) {

        LinearLayout userItem = (LinearLayout) QQData.m_actHome.getLayoutInflater().inflate(R.layout.user_list_item, null);

        LinearLayout LL1 = (LinearLayout) userItem.getChildAt(0);
        LinearLayout LLInfo = (LinearLayout) LL1.getChildAt(1);

        final de.hdodenhof.circleimageview.CircleImageView civPhoto = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(0);
        TextView tvName = (TextView) LLInfo.getChildAt(0);
        TextView tvNick = (TextView) LLInfo.getChildAt(1);

        if (!pp.equals("-1")) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(pp), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    civPhoto.setImageBitmap(loadedImage);
                }
            });
        }

        tvName.setText(name);
        tvNick.setText("@" + nick);

        civPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (uid == QQData.m_iUID) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.setFragment(4);
                    return;
                }

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

                if (uid == QQData.m_iUID) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.setFragment(4);
                    return;
                }

                Bundle b = new Bundle();
                b.putInt("target_uid", uid);
                Intent it = new Intent(QQData.m_actHome, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (uid == QQData.m_iUID) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.setFragment(4);
                    return;
                }

                Bundle b = new Bundle();
                b.putInt("target_uid", uid);
                Intent it = new Intent(QQData.m_actHome, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });


        llSearchResults.addView(userItem);
    }

    public void setSearching(String s) {
        edSearch.setText(s);
    }
}