package com.quiqueapp.qq;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.core.imageaware.NonViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

/**
 * Created by user on 17.11.2015.
 */
public class F_PostQuestion extends Fragment {

    private boolean bLocation = true;

    public Bitmap bmQuestionImage = null;
    public Bitmap[] bmPostImages = new Bitmap[8];

    private ImageButton ibPostQuestion;
    public FloatingActionMenu famPost;

    private int iOptionCount = 0;
    private EditText edQuestion, edTags;
    private LinearLayout llPostOptions, llPostQuestionPredict;
    private ImageButton ibAddPostOption, ibRemovePostOption, ibPostLocation;

    private ImageButton ibSetQuestionImage;
    private CircleImageView civQuestionImage;

    private Snackbar sbPosting = null;

    private int iSearchType = 0;
    private String sSearchQuery = null;
    private boolean bConcatQuery = false;

    private CountDownTimer cdt = new CountDownTimer(1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            switch (iSearchType) {
                case 1:
                    sendSearchName(sSearchQuery);
                    break;
                case 2:
                    sendSearchTag(sSearchQuery);
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View v = inflater.inflate(R.layout.f_postquestion, container, false);

        ibPostQuestion = (ImageButton) v.findViewById(R.id.ibPostQuestion);
        createPostButton();

        edQuestion = (EditText) v.findViewById(R.id.edQuestion);
        edTags = (EditText) v.findViewById(R.id.edTags);

        edQuestion.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkSearchName(s.toString(), start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        edTags.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkSearchTag(s.toString(), start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        llPostOptions = (LinearLayout) v.findViewById(R.id.llPostOptions);
        llPostQuestionPredict = (LinearLayout) v.findViewById(R.id.llPostQuestionPredict);
        ibAddPostOption = (ImageButton) v.findViewById(R.id.ibAddPostOption);
        ibRemovePostOption = (ImageButton) v.findViewById(R.id.ibRemovePostOption);
        ibPostLocation = (ImageButton) v.findViewById(R.id.ibPostLocation);

        ibSetQuestionImage = (ImageButton) v.findViewById(R.id.ibSetQuestionImage);
        civQuestionImage = (CircleImageView) v.findViewById(R.id.civQuestionImage);

        f_addPostOption(null);
        f_addPostOption(null);
        f_addPostOption(null);
        f_addPostOption(null);

        return v;
    }

    public void f_addPostOption(View v) {
        final LinearLayout poItem = (LinearLayout) QQData.m_actHome.getLayoutInflater().inflate(R.layout.post_option_item, null);
        LinearLayout LL1 = (LinearLayout) poItem.getChildAt(0);

        ImageButton ibPic = (ImageButton) LL1.getChildAt(0);
        de.hdodenhof.circleimageview.CircleImageView civImage = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(1);

        llPostOptions.addView(poItem, llPostOptions.getChildCount() - 1);
        iOptionCount++;

        final int iOption = iOptionCount - 1;
        ibPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putInt("option", iOption);
                Intent it = new Intent(QQData.m_actHome, SetOptionImageActivity.class);
                it.putExtras(b);
                Bundle fade = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_fade_in, R.anim.act_fade_out).toBundle();
                startActivity(it, fade);
            }
        });

        civImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putInt("option", iOption);
                Intent it = new Intent(QQData.m_actHome, SetOptionImageActivity.class);
                it.putExtras(b);
                Bundle fade = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_fade_in, R.anim.act_fade_out).toBundle();
                startActivity(it, fade);
            }
        });

        if (iOptionCount == 8)
            ibAddPostOption.setVisibility(View.GONE);

        if (ibRemovePostOption.getVisibility() == View.GONE)
            ibRemovePostOption.setVisibility(View.VISIBLE);
    }

    public void f_removePostOption(View v) {
        LinearLayout poItem = (LinearLayout) llPostOptions.getChildAt(llPostOptions.getChildCount() - 2);
        llPostOptions.removeView(poItem);
        iOptionCount--;

        if (bmPostImages[iOptionCount] != null) {
            bmPostImages[iOptionCount].recycle();
            bmPostImages[iOptionCount] = null;
        }

        if (iOptionCount == 0)
            ibRemovePostOption.setVisibility(View.GONE);

        if (ibAddPostOption.getVisibility() == View.GONE)
            ibAddPostOption.setVisibility(View.VISIBLE);
    }

    public void clearPostOptions() {
        while (iOptionCount > 0)
            f_removePostOption(null);

        edQuestion.setText("");
        edTags.setText("");

        if (bmQuestionImage != null) {
            bmQuestionImage.recycle();
            bmQuestionImage = null;
        }

        bLocation = true;
        ibPostLocation.setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_post_location_on));
        ibPostLocation.setAlpha(1f);

        civQuestionImage.setImageBitmap(null);
        civQuestionImage.setVisibility(View.GONE);
        ibSetQuestionImage.setVisibility(View.VISIBLE);

        f_addPostOption(null);
        f_addPostOption(null);
        f_addPostOption(null);
        f_addPostOption(null);
    }

    public String getBase64(Bitmap bmPhoto) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bmPhoto.compress(Bitmap.CompressFormat.JPEG, 100, os);
        return Base64.encodeToString(os.toByteArray(), Base64.DEFAULT);
    }

    public void f_goPostQuestion(int iType) {

        boolean bPrivate = false, bAnonym = false;
        if (iType == 0)
            bPrivate = true;
        else if (iType == 2)
            bAnonym = true;

        String sQuestion = edQuestion.getText().toString().trim();
        String sTags = edTags.getText().toString().trim();

        if (sQuestion.length() == 0) {
            QQData.showError(QQData.m_actHome, "Soru girilmemiş");
            return;
        }

        String sQuestionImage = "0";
        String[] optionParams = {"", "", "", "", "", "", "", ""};
        String[] imageParams = {"0", "0", "0", "0", "0", "0", "0", "0"};
        int iRealOptionCount = 0;

        if (iOptionCount > 0) {
            for (int i = 0; i < iOptionCount; i++) {
                LinearLayout poItem = (LinearLayout) llPostOptions.getChildAt(i);
                LinearLayout LL1 = (LinearLayout) poItem.getChildAt(0);
                EditText edOption = (EditText) LL1.getChildAt(2);

                if (edOption.getText().toString().trim().length() > 0) {
                    optionParams[i] = edOption.getText().toString().trim();

                    if (bmPostImages[i] != null)
                        imageParams[i] = getBase64(bmPostImages[i]);

                    iRealOptionCount++;
                } else {
                    QQData.showError(QQData.m_actHome, "Açık seçenekleri kapatmalı ya da doldurmalısınız");
                    return;
                }
            }
        }

        if (bmQuestionImage != null)
            sQuestionImage = getBase64(bmQuestionImage);

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qtext", sQuestion)
                .add("anonym", String.valueOf((bAnonym) ? 1 : 0))
                .add("private", String.valueOf((bPrivate) ? 1 : 0))
                .add("opcount", String.valueOf(iRealOptionCount))
                .add("tags", sTags)
                .add("qimage", sQuestionImage)
                .add("op1", optionParams[0])
                .add("op2", optionParams[1])
                .add("op3", optionParams[2])
                .add("op4", optionParams[3])
                .add("op5", optionParams[4])
                .add("op6", optionParams[5])
                .add("op7", optionParams[6])
                .add("op8", optionParams[7])
                .add("img1", imageParams[0])
                .add("img2", imageParams[1])
                .add("img3", imageParams[2])
                .add("img4", imageParams[3])
                .add("img5", imageParams[4])
                .add("img6", imageParams[5])
                .add("img7", imageParams[6])
                .add("img8", imageParams[7]);

        final PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSendQAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                sbPosting.dismiss();
                getResult_PostQuestion(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                sbPosting.dismiss();
                e.printStackTrace();
                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");
            }
        });

        sbPosting = QQData.showInfoInfinite(QQData.m_actHome, "Soru paylaşılıyor...");

        if (bLocation && SmartLocation.with(QQData.m_actHome).location().state().isAnyProviderAvailable()) {
            SmartLocation.with(QQData.m_actHome).location().oneFix()
                    .start(new OnLocationUpdatedListener() {
                        @Override
                        public void onLocationUpdated(Location location) {
                            pr.pm.add("latitude", String.valueOf(location.getLatitude()));
                            pr.pm.add("longitude", String.valueOf(location.getLongitude()));
                            SmartLocation.with(QQData.m_actHome).location().stop();
                            QQData.pushPostRequest(pr);
                        }
                    });
        } else {
            pr.pm.add("latitude", "0");
            pr.pm.add("longitude", "0");
            QQData.pushPostRequest(pr);
        }
    }

    public void getResult_PostQuestion(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 201) {   // post girildi
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    clearPostOptions();

                    QQData.MyInfo.iQuestions++;
                    QQData.m_actHome.fMyProfile.f_updateProfileViews();

                    JSONObject jsonQuestion = new JSONObject(jsonRet.getString("question"));
                    getQuestionParams(jsonQuestion);

                    QQData.m_actHome.setFragment(0);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error

                QQData.showError(QQData.m_actHome, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { // sid error

                QQData.m_actHome.doLogout();

            }
        }
    }

    public void getQuestionParams(JSONObject jsonQuestion) {
        try {
            int userID = Integer.parseInt(jsonQuestion.getString("uid"));
            String pp = jsonQuestion.getString("pp");
            String sNick = jsonQuestion.getString("nick");
            String sName = jsonQuestion.getString("name");
            int QID = Integer.parseInt(jsonQuestion.getString("qid"));
            String sQuestion = jsonQuestion.getString("question");
            int q_anonym = Integer.parseInt(jsonQuestion.getString("anonym"));
            int q_private = Integer.parseInt(jsonQuestion.getString("private"));
            int opcount = Integer.parseInt(jsonQuestion.getString("options"));
            int reqs = Integer.parseInt(jsonQuestion.getString("reqs"));
            int favs = Integer.parseInt(jsonQuestion.getString("favs"));
            String sTags = jsonQuestion.getString("tags");
            int locked = Integer.parseInt(jsonQuestion.getString("locked"));
            int past_hour = Integer.parseInt(jsonQuestion.getString("phour"));

            JSONArray jsonOptions = new JSONArray(jsonQuestion.getString("s_options"));
            String[] s_options = null;
            if (opcount > 0) {
                s_options = new String[opcount];
                for (int j = 0; j < opcount; j++) {
                    s_options[j] = jsonOptions.get(j + 1).toString();
                }
            }

            JSONArray jsonVotes = new JSONArray(jsonQuestion.getString("votes"));
            Integer[] votes = null;
            if (opcount > 0) {
                votes = new Integer[opcount];
                for (int k = 0; k < opcount; k++) {
                    votes[k] = jsonVotes.getInt(k + 1);
                }
            }

            JSONArray jsonImages = new JSONArray(jsonQuestion.getString("images"));
            String[] simages = null;
            if (opcount > 0) {
                simages = new String[opcount];
                for (int l = 0; l < opcount; l++) {
                    simages[l] = jsonImages.get(l + 1).toString();
                }
            }

            int faved = Integer.parseInt(jsonQuestion.getString("faved"));
            int reported = Integer.parseInt(jsonQuestion.getString("reported"));
            int voted = Integer.parseInt(jsonQuestion.getString("voted"));
            int votenum = Integer.parseInt(jsonQuestion.getString("votenum"));
            int comments = Integer.parseInt(jsonQuestion.getString("comments"));
            String date = jsonQuestion.getString("date");
            String qimage = jsonQuestion.getString("qimage");
            int reposted = Integer.parseInt(jsonQuestion.getString("reposted"));

            Question q = new Question(
                    userID,
                    pp,
                    sNick,
                    sName,
                    QID,
                    sQuestion,
                    sTags,
                    q_anonym,
                    q_private,
                    opcount,
                    reqs,
                    favs,
                    locked,
                    past_hour,
                    s_options,
                    votes,
                    simages,
                    qimage,
                    faved,
                    reported,
                    voted,
                    comments,
                    date,
                    votenum,
                    reposted,
                    -1,
                    "");

            Question q2 = new Question(
                    userID,
                    pp,
                    sNick,
                    sName,
                    QID,
                    sQuestion,
                    sTags,
                    q_anonym,
                    q_private,
                    opcount,
                    reqs,
                    favs,
                    locked,
                    past_hour,
                    s_options,
                    votes,
                    simages,
                    qimage,
                    faved,
                    reported,
                    voted,
                    comments,
                    date,
                    votenum,
                    reposted,
                    -1,
                    "");

            QQData.m_actHome.fMyProfile.f_addQuestion(q, 0);
            QQData.m_actHome.fHome.fHomeFollowing.addQuestion(q2, 0);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void createPostButton() {

        int pDimenSize36 = (int) QQData.m_actHome.getResources().getDimension(R.dimen.post_button_size36);

        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(QQData.m_actHome);

        ImageView ivPostAll = new ImageView(QQData.m_actHome);
        ivPostAll.setImageDrawable(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_button_post_all));
        SubActionButton sabPostAll = itemBuilder.setContentView(ivPostAll).setLayoutParams(new FrameLayout.LayoutParams(pDimenSize36, pDimenSize36)).build();
        sabPostAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                f_goPostQuestion(1);
            }
        });

        ImageView ivPostAnonym = new ImageView(QQData.m_actHome);
        ivPostAnonym.setImageDrawable(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_button_post_anonym));
        SubActionButton sabPostAnonym = itemBuilder.setContentView(ivPostAnonym).setLayoutParams(new FrameLayout.LayoutParams(pDimenSize36, pDimenSize36)).build();
        sabPostAnonym.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                f_goPostQuestion(2);
            }
        });

        ImageView ivPostPrivate = new ImageView(QQData.m_actHome);
        ivPostPrivate.setImageDrawable(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_button_post_private));
        SubActionButton sabPostPrivate = itemBuilder.setContentView(ivPostPrivate).setLayoutParams(new FrameLayout.LayoutParams(pDimenSize36, pDimenSize36)).build();
        sabPostPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                f_goPostQuestion(0);
            }
        });

        famPost = new FloatingActionMenu.Builder(QQData.m_actHome)
                .setEndAngle(360)
                .setRadius(180)
                .addSubActionView(sabPostPrivate)
                .addSubActionView(sabPostAll)
                .addSubActionView(sabPostAnonym)
                .attachTo(ibPostQuestion)
                .build();
    }

    public void updateImage(int iOption, Bitmap bmImage, Bitmap bmThumb) {
        LinearLayout poItem = (LinearLayout) llPostOptions.getChildAt(iOption);
        LinearLayout LL1 = (LinearLayout) poItem.getChildAt(0);
        ImageButton ibPic = (ImageButton) LL1.getChildAt(0);
        de.hdodenhof.circleimageview.CircleImageView civImage = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(1);

        if (bmImage != null) {
            if (bmPostImages[iOption] != null)
                bmPostImages[iOption].recycle();
            bmPostImages[iOption] = bmImage;
            ibPic.setVisibility(View.GONE);
            civImage.setVisibility(View.VISIBLE);
            civImage.setImageBitmap(bmThumb);
        } else {
            if (bmPostImages[iOption] != null)
                bmPostImages[iOption].recycle();
            bmPostImages[iOption] = null;
            civImage.setImageBitmap(null);
            civImage.setVisibility(View.GONE);
            ibPic.setVisibility(View.VISIBLE);
        }
    }

    public void f_setQuestionImage(View v) {
        Intent it = new Intent(QQData.m_actHome, SetQuestionImageActivity.class);
        Bundle fade = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_fade_in, R.anim.act_fade_out).toBundle();
        startActivity(it, fade);
    }

    public void updateQuestionImage(Bitmap bmImage, Bitmap bmThumb) {
        if (bmImage != null) {
            if (bmQuestionImage != null)
                bmQuestionImage.recycle();
            bmQuestionImage = bmImage;
            civQuestionImage.setImageBitmap(bmThumb);
            ibSetQuestionImage.setVisibility(View.GONE);
            civQuestionImage.setVisibility(View.VISIBLE);
        } else {
            if (bmQuestionImage != null)
                bmQuestionImage.recycle();
            bmQuestionImage = null;
            civQuestionImage.setImageBitmap(null);
            civQuestionImage.setVisibility(View.GONE);
            ibSetQuestionImage.setVisibility(View.VISIBLE);
        }
    }

    public void f_togglePostLocation(View v) {
        if (bLocation) {
            bLocation = false;
            ibPostLocation.setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_post_location_off));
            ibPostLocation.setAlpha(0.5f);
        } else {
            bLocation = true;
            ibPostLocation.setBackground(QQData.m_actHome.getResources().getDrawable(R.drawable.ic_post_location_on));
            ibPostLocation.setAlpha(1f);
        }
    }

    public void checkSearchName(String s, int start, int before, int count) {
        if (s.trim().length() > 0) {

            if (!bConcatQuery) {
                if (s.charAt(s.length() - 1) == '@') {
                    sSearchQuery = "";
                    bConcatQuery = true;
                }
            } else {

                if (before == 0) {  //yazarkene

                    if (s.charAt(s.length() - 1) == ' ') {
                        sSearchQuery = "";
                        bConcatQuery = false;
                        llPostQuestionPredict.setVisibility(View.GONE);
                    } else {
                        cdt.cancel();
                        sSearchQuery = sSearchQuery.concat(String.valueOf(s.charAt(s.length() - 1)));
                        iSearchType = 1;
                        cdt.start();
                        llPostQuestionPredict.setVisibility(View.VISIBLE);
                    }

                } else {    // silerkene

                    if (s.charAt(s.length() - 1) == ' ') {
                        sSearchQuery = "";
                        bConcatQuery = false;
                        llPostQuestionPredict.setVisibility(View.GONE);
                    } else {
                        if (s.charAt(s.length() - 1) == '@') {
                            sSearchQuery = "";
                        } else {
                            cdt.cancel();
                            sSearchQuery = sSearchQuery.substring(0, sSearchQuery.length() - 1);
                            iSearchType = 1;
                            cdt.start();
                            llPostQuestionPredict.setVisibility(View.VISIBLE);
                        }
                    }

                }
            }

        } else {
            sSearchQuery = "";
            bConcatQuery = false;
            llPostQuestionPredict.setVisibility(View.GONE);
        }
    }

    public void checkSearchTag(String s, int start, int before, int count) {
        if (s.trim().length() > 0) {

            if (!bConcatQuery) {
                if (s.charAt(s.length() - 1) == '#') {
                    sSearchQuery = "";
                    bConcatQuery = true;
                }
            } else {

                if (before == 0) {  //yazarkene

                    if (s.charAt(s.length() - 1) == ' ') {
                        sSearchQuery = "";
                        bConcatQuery = false;
                        llPostQuestionPredict.setVisibility(View.GONE);
                    } else {
                        cdt.cancel();
                        sSearchQuery = sSearchQuery.concat(String.valueOf(s.charAt(s.length() - 1)));
                        iSearchType = 2;
                        cdt.start();
                        llPostQuestionPredict.setVisibility(View.VISIBLE);
                    }

                } else {    // silerkene

                    if (s.charAt(s.length() - 1) == ' ') {
                        sSearchQuery = "";
                        bConcatQuery = false;
                        llPostQuestionPredict.setVisibility(View.GONE);
                    } else {
                        if (s.charAt(s.length() - 1) == '#') {
                            sSearchQuery = "";
                        } else {
                            cdt.cancel();
                            sSearchQuery = sSearchQuery.substring(0, sSearchQuery.length() - 1);
                            iSearchType = 2;
                            cdt.start();
                            llPostQuestionPredict.setVisibility(View.VISIBLE);
                        }
                    }

                }
            }

        } else {
            sSearchQuery = "";
            bConcatQuery = false;
            llPostQuestionPredict.setVisibility(View.GONE);
        }
    }

    public void sendSearchName(String name) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("name", name)
                .add("p", "0");

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSearchNameAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_NameList(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
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
                    llPostQuestionPredict.removeAllViews();

                    if (!jsonRet.getString("users").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("users"));
                        getNameList(jsonList);
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
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

    public void addNameUser(final int uid, final String nick, String name, String pp) {

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
                putPredictedNick(nick);
            }
        });

        tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                putPredictedNick(nick);
            }
        });

        tvNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                putPredictedNick(nick);
            }
        });


        llPostQuestionPredict.addView(userItem);
    }

    public void putPredictedNick(String nick) {
        String q = edQuestion.getText().toString();

        int prefix = q.lastIndexOf('@');
        if (prefix != -1) {
            q = q.substring(0, prefix);
            q = q.concat("@" + nick);
            edQuestion.setText(q);
            edQuestion.setSelection(q.length());
        }
        sSearchQuery = "";
        bConcatQuery = false;
        llPostQuestionPredict.setVisibility(View.GONE);
    }

    public void sendSearchTag(String tag) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("tag", tag)
                .add("p", "0");

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSearchTagAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_TagList(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
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
                    llPostQuestionPredict.removeAllViews();

                    if (!jsonRet.getString("tags").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("tags"));
                        getSearchTagList(jsonList);
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
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
                putPredictedTag(tag);
            }
        });

        tvCnt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                putPredictedTag(tag);
            }
        });

        llPostQuestionPredict.addView(tagItem);
    }

    public void putPredictedTag(String tag) {
        String t = edTags.getText().toString();

        int prefix = t.lastIndexOf('#');
        if (prefix != -1) {
            t = t.substring(0, prefix);
            t = t.concat(tag);
            edTags.setText(t);
            edTags.setSelection(t.length());
        }
        sSearchQuery = "";
        bConcatQuery = false;
        llPostQuestionPredict.setVisibility(View.GONE);
    }
}