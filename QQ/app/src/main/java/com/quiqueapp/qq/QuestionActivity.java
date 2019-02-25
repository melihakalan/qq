package com.quiqueapp.qq;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.faradaj.blurbehind.BlurBehind;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class QuestionActivity extends Activity {

    private int iQID;
    private Question mQ = null;

    private SwipeRefreshLayout srlQuestion;
    private InteractiveScrollView isvQuestion;
    private LinearLayout llQuestionLoading, llQuestionLayout, llQuestionBody, llQuestionComments, llSendComment, llPostCommentPredict;
    private EditText edComment;
    private Map<Integer, CommentItem> cList = new HashMap<Integer, CommentItem>();

    private int iScrollLoadPage = 0;
    private boolean bScrollLoadProcess = false;

    private ImageButton ibQuestionRemoveTopbar, ibQuestionReportTopbar, ibQuestionFBTopbar, ibQuestionTWTopbar;

    private String sSearchQuery = null;
    private boolean bConcatQuery = false;

    private CountDownTimer cdt = new CountDownTimer(1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            sendSearchName(sSearchQuery);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        QQData.m_lActivityList.add(QuestionActivity.this);

        srlQuestion = (SwipeRefreshLayout) findViewById(R.id.srlQuestion);
        isvQuestion = (InteractiveScrollView) findViewById(R.id.isvQuestion);

        llQuestionLoading = (LinearLayout) findViewById(R.id.llQuestionLoading);
        llQuestionLayout = (LinearLayout) findViewById(R.id.llQuestionLayout);
        llQuestionBody = (LinearLayout) findViewById(R.id.llQuestionBody);
        llQuestionComments = (LinearLayout) findViewById(R.id.llQuestionComments);
        llSendComment = (LinearLayout) findViewById(R.id.llSendComment);
        llPostCommentPredict = (LinearLayout) findViewById(R.id.llPostCommentPredict);

        edComment = (EditText) findViewById(R.id.edComment);

        edComment.addTextChangedListener(new TextWatcher() {
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

        ibQuestionRemoveTopbar = (ImageButton) findViewById(R.id.ibQuestionRemoveTopbar);
        ibQuestionReportTopbar = (ImageButton) findViewById(R.id.ibQuestionReportTopbar);
        ibQuestionFBTopbar = (ImageButton) findViewById(R.id.ibQuestionFBTopbar);
        ibQuestionTWTopbar = (ImageButton) findViewById(R.id.ibQuestionTWTopbar);

        srlQuestion.setProgressBackgroundColorSchemeColor(Color.parseColor("#EEEEEE"));
        srlQuestion.setColorSchemeColors(Color.parseColor("#58ACFA"));
        srlQuestion.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                clearQuestion();
                getQuestion();
            }
        });

        isvQuestion.setOnBottomReachedListener(new InteractiveScrollView.OnBottomReachedListener() {
            @Override
            public void onBottomReached() {
                if (!bScrollLoadProcess) {
                    srlQuestion.setRefreshing(true);
                    bScrollLoadProcess = true;
                    iScrollLoadPage++;
                    getComments();
                }
            }
        });

        getQuestionInfo();
        getQuestion();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_slide_in_right, R.anim.act_slide_out_right);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(QuestionActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(QuestionActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(QuestionActivity.this);
        }
    }

    public void goHome(View v) {
        QQData.clearAllActivities();
    }

    public void getQuestionInfo() {
        Bundle b = getIntent().getExtras();
        iQID = b.getInt("qid");
    }

    public void clearQuestion() {
        iScrollLoadPage = 0;
        cList.clear();
        llQuestionComments.removeAllViews();
        llQuestionBody.removeAllViews();
        llSendComment.setVisibility(View.GONE);
    }

    public void getQuestion() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(iQID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sGetQuestionAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlQuestion.isRefreshing())
                    srlQuestion.setRefreshing(false);

                getResult_GetQuestion(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlQuestion.isRefreshing())
                    srlQuestion.setRefreshing(false);

                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_GetQuestion(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 651) {   // get question details
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));

                    if (!jsonRet.getString("question").equals("0")) {
                        JSONObject jsonQuestion = new JSONObject(jsonRet.getString("question"));
                        getQuestionParams(jsonQuestion);
                    }

                    getComments();

                    llQuestionLoading.setVisibility(View.GONE);
                    llQuestionLayout.setVisibility(View.VISIBLE);
                    llSendComment.setVisibility(View.VISIBLE);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
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

            mQ = new Question(
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

            addQuestion(mQ);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addQuestion(final Question q) {

        LinearLayout qItem = (LinearLayout) getLayoutInflater().inflate(R.layout.question_item, null);

        LinearLayout llRequest = (LinearLayout) qItem.getChildAt(0);
        ImageView ivRequestIcon = (ImageView) llRequest.getChildAt(0);
        TextView tvRequest = (TextView) llRequest.getChildAt(1);

        LinearLayout llItemContainer = (LinearLayout) qItem.getChildAt(1);

        LinearLayout llInfoHeader = (LinearLayout) llItemContainer.getChildAt(0);
        final de.hdodenhof.circleimageview.CircleImageView civPhoto = (de.hdodenhof.circleimageview.CircleImageView) llInfoHeader.getChildAt(0);
        LinearLayout llProfileInfo = (LinearLayout) llInfoHeader.getChildAt(1);
        TextView tvName = (TextView) llProfileInfo.getChildAt(0);
        TextView tvNick = (TextView) llProfileInfo.getChildAt(1);
        RelativeLayout rlQuestionState = (RelativeLayout) llInfoHeader.getChildAt(2);
        FrameLayout flQuestionTimer = (FrameLayout) rlQuestionState.getChildAt(0);
        ImageView ivQuestionTimer = (ImageView) flQuestionTimer.getChildAt(0);
        TextView tvQuestionTimer = (TextView) flQuestionTimer.getChildAt(1);
        ImageView ivQuestionLocked = (ImageView) flQuestionTimer.getChildAt(2);
        ImageView ivQuestionTargetState = (ImageView) rlQuestionState.getChildAt(1);
        TextView tvVoteCount = (TextView) rlQuestionState.getChildAt(2);
        TextView tvDate = (TextView) rlQuestionState.getChildAt(3);

        LinearLayout llQuestionInfo = (LinearLayout) llItemContainer.getChildAt(2);
        ImageButton ibQuestionImage = (ImageButton) llQuestionInfo.getChildAt(0);
        LinearLayout llQuestionTexts = (LinearLayout) llQuestionInfo.getChildAt(2);
        TextView tvQuestion = (TextView) llQuestionTexts.getChildAt(0);
        TextView tvTags = (TextView) llQuestionTexts.getChildAt(1);

        LinearLayout llQuestionOptions = (LinearLayout) llItemContainer.getChildAt(3);
        LinearLayout[] llQuestionOption = null;
        CircleImageView[] civOptionImages = null;
        TextView[] tvQuestionOption = null;
        ImageView[] ivQuestionSelectedIcon = null;
        TextView[] tvQuestionOptionPercent = null;
        if (q.iOptionCount > 0) {
            llQuestionOption = new LinearLayout[q.iOptionCount];
            civOptionImages = new CircleImageView[q.iOptionCount];
            tvQuestionOption = new TextView[q.iOptionCount];
            ivQuestionSelectedIcon = new ImageView[q.iOptionCount];
            tvQuestionOptionPercent = new TextView[q.iOptionCount];

            for (int i = 0; i < q.iOptionCount; i++) {
                llQuestionOption[i] = (LinearLayout) llQuestionOptions.getChildAt(i);
                civOptionImages[i] = (CircleImageView) llQuestionOption[i].getChildAt(0);
                tvQuestionOption[i] = (TextView) llQuestionOption[i].getChildAt(1);
                ivQuestionSelectedIcon[i] = (ImageView) llQuestionOption[i].getChildAt(2);
                tvQuestionOptionPercent[i] = (TextView) llQuestionOption[i].getChildAt(3);
            }
        }

        LinearLayout llQuestionIcons = (LinearLayout) llItemContainer.getChildAt(4);
        LinearLayout llFavIcon = (LinearLayout) llQuestionIcons.getChildAt(0);
        ImageButton ibFavIcon = (ImageButton) llFavIcon.getChildAt(0);
        LinearLayout llCommentIcon = (LinearLayout) llQuestionIcons.getChildAt(1);
        ImageButton ibCommentIcon = (ImageButton) llCommentIcon.getChildAt(0);
        LinearLayout llReqIcon = (LinearLayout) llQuestionIcons.getChildAt(2);
        ImageButton ibReqIcon = (ImageButton) llReqIcon.getChildAt(0);

        LinearLayout llFavReqCount = (LinearLayout) llItemContainer.getChildAt(5);
        LinearLayout llFavCount = (LinearLayout) llFavReqCount.getChildAt(0);
        TextView tvFavCount = (TextView) llFavCount.getChildAt(0);
        LinearLayout llCommentCount = (LinearLayout) llFavReqCount.getChildAt(1);
        TextView tvCommentCount = (TextView) llCommentCount.getChildAt(0);
        LinearLayout llReqCount = (LinearLayout) llFavReqCount.getChildAt(2);
        TextView tvReqCount = (TextView) llReqCount.getChildAt(0);

        final FrameLayout rlExpandIcon = (FrameLayout) llItemContainer.getChildAt(6);
        ImageButton ibExpandIcon = (ImageButton) rlExpandIcon.getChildAt(0);

        // Set

        if (!q.sQuestionImage.equals("0")) {
            Animation blink = AnimationUtils.loadAnimation(QuestionActivity.this, R.anim.anim_fade_blink);
            ibQuestionImage.setVisibility(View.VISIBLE);
            ibQuestionImage.startAnimation(blink);

            ibQuestionImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BlurBehind.getInstance().execute(QuestionActivity.this, new Runnable() {
                        @Override
                        public void run() {
                            Bundle b = new Bundle();
                            b.putString("image", q.sQuestionImage);
                            Intent it = new Intent(QuestionActivity.this, ShowQuestionImageActivity.class);
                            it.putExtras(b);
                            Bundle fade = ActivityOptions.makeCustomAnimation(QuestionActivity.this, R.anim.act_fade_in, R.anim.act_fade_out).toBundle();
                            startActivity(it, fade);
                        }
                    });
                }
            });

        }

        if (q.iUID == QQData.m_iUID) {
            ibFavIcon.setClickable(false);
            ibReqIcon.setClickable(false);
            ibQuestionRemoveTopbar.setVisibility(View.VISIBLE);
        } else {
            if (q.iReposted == 1)
                ibQuestionRemoveTopbar.setVisibility(View.VISIBLE);
            else
                ibQuestionReportTopbar.setVisibility(View.VISIBLE);
        }

        if (q.iAnonym == 0) {
            if (!q.sPP.equals("-1")) {
                ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(q.sPP), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        civPhoto.setImageBitmap(loadedImage);
                    }
                });
            }
        }

        tvDate.setText(QQData.getDateDiff(q.sDate) + " |");
        if (q.iAnonym == 0) {
            tvName.setText(q.sName);
            tvNick.setText("@" + q.sNick);
        } else {
            tvName.setText("Anonim");
            tvNick.setText("");
            civPhoto.setImageDrawable(getResources().getDrawable(R.drawable.ic_pp_anonym_64));
            tvName.setAlpha(0.5f);
        }
        tvQuestion.setText(spanTaggedUsers(q.sQuestion), TextView.BufferType.SPANNABLE);
        tvQuestion.setMovementMethod(LinkMovementMethod.getInstance());
        tvTags.setText(q.sTags);

        if (q.sTags.trim().length() > 0) {
            tvTags.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.fSearch.setSearching(q.sTags);
                    QQData.m_actHome.setFragment(1);
                }
            });
        }

        flQuestionTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (q.iLocked == 0)
                    QQData.showInfo(QuestionActivity.this, "Bu soru " + (24 - q.iPastHour) + " saat sonra kilitlenecek");
            }
        });

        if (q.iAnonym == 0) {
            civPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (q.iUID == QQData.m_iUID) {
                        QQData.clearAllActivities();
                        QQData.m_actHome.setFragment(4);
                        return;
                    }

                    Bundle b = new Bundle();
                    b.putInt("target_uid", q.iUID);
                    Intent it = new Intent(QuestionActivity.this, UserProfileActivity.class);
                    it.putExtras(b);
                    Bundle translate = ActivityOptions.makeCustomAnimation(QuestionActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                    startActivity(it, translate);
                }
            });

            tvName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (q.iUID == QQData.m_iUID) {
                        QQData.clearAllActivities();
                        QQData.m_actHome.setFragment(4);
                        return;
                    }

                    Bundle b = new Bundle();
                    b.putInt("target_uid", q.iUID);
                    Intent it = new Intent(QuestionActivity.this, UserProfileActivity.class);
                    it.putExtras(b);
                    Bundle translate = ActivityOptions.makeCustomAnimation(QuestionActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                    startActivity(it, translate);
                }
            });

            tvNick.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (q.iUID == QQData.m_iUID) {
                        QQData.clearAllActivities();
                        QQData.m_actHome.setFragment(4);
                        return;
                    }

                    Bundle b = new Bundle();
                    b.putInt("target_uid", q.iUID);
                    Intent it = new Intent(QuestionActivity.this, UserProfileActivity.class);
                    it.putExtras(b);
                    Bundle translate = ActivityOptions.makeCustomAnimation(QuestionActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                    startActivity(it, translate);
                }
            });
        }

        if (q.iAnonym == 1) {
            ivQuestionTargetState.setImageDrawable(getResources().getDrawable(R.drawable.ic_question_anonym));
        } else if (q.iPrivate == 1) {
            ivQuestionTargetState.setImageDrawable(getResources().getDrawable(R.drawable.ic_question_private));
        }

        if (q.iLocked == 1) {
            ivQuestionTimer.setVisibility(View.GONE);
            tvQuestionTimer.setVisibility(View.GONE);
            ivQuestionLocked.setVisibility(View.VISIBLE);
        } else {
            tvQuestionTimer.setText(String.valueOf(24 - q.iPastHour));
            Animation blink = AnimationUtils.loadAnimation(QuestionActivity.this, R.anim.anim_fade_blink2);
            flQuestionTimer.startAnimation(blink);
        }

        if (q.iOptionCount > 0) {

            tvVoteCount.setText(String.valueOf(q.iTotalVotes));
            tvVoteCount.setVisibility(View.VISIBLE);

            for (int i = 0; i < q.iOptionCount; i++) {

                if (!q.sImages[i].equals("0")) {
                    final CircleImageView civOptionImage = civOptionImages[i];
                    civOptionImage.setVisibility(View.VISIBLE);

                    final int imgno = i;
                    civOptionImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            BlurBehind.getInstance().execute(QuestionActivity.this, new Runnable() {
                                @Override
                                public void run() {
                                    Bundle b = new Bundle();
                                    b.putString("image", q.sImages[imgno]);
                                    Intent it = new Intent(QuestionActivity.this, ShowOptionImageActivity.class);
                                    it.putExtras(b);
                                    Bundle fade = ActivityOptions.makeCustomAnimation(QuestionActivity.this, R.anim.act_fade_in, R.anim.act_fade_out).toBundle();
                                    startActivity(it, fade);
                                }
                            });
                        }
                    });

                    ImageLoader.getInstance().displayImage(QQData.m_sOptionImageAdr + QQData.getThumbName(q.sImages[i]), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            civOptionImage.setImageBitmap(loadedImage);
                        }
                    });
                }

                tvQuestionOption[i].setText(q.sOptions[i]);
                tvQuestionOptionPercent[i].setText("%" + String.valueOf(QQData.getVotePercent(q.iTotalVotes, q.iVotes[i])));
                if (q.iUID == QQData.m_iUID || q.iVoted == 1 || q.iLocked == 1)
                    tvQuestionOptionPercent[i].setVisibility(View.VISIBLE);

                llQuestionOption[i].setVisibility(View.VISIBLE);

                final LinearLayout fnllQuestionOption = llQuestionOption[i];

                fnllQuestionOption.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (q.iUID == QQData.m_iUID || q.iVoted == 1 || q.iLocked == 1)
                            return false;

                        if (event.getAction() == MotionEvent.ACTION_DOWN)
                            fnllQuestionOption.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_question_option_selected));
                        else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                            fnllQuestionOption.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_question_option_normal));
                        return false;
                    }
                });

                tvQuestionOption[i].setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (q.iUID == QQData.m_iUID || q.iVoted == 1 || q.iLocked == 1)
                            return false;

                        if (event.getAction() == MotionEvent.ACTION_DOWN)
                            fnllQuestionOption.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_question_option_selected));
                        else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                            fnllQuestionOption.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_question_option_normal));
                        return false;
                    }
                });

                tvQuestionOptionPercent[i].setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (q.iUID == QQData.m_iUID || q.iVoted == 1 || q.iLocked == 1)
                            return false;

                        if (event.getAction() == MotionEvent.ACTION_DOWN)
                            fnllQuestionOption.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_question_option_selected));
                        else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                            fnllQuestionOption.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_question_option_normal));
                        return false;
                    }
                });

                final int optidx = i;

                fnllQuestionOption.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (q.iUID == QQData.m_iUID || q.iVoted == 1 || q.iLocked == 1)
                            return;

                        doVote(q, optidx);
                    }
                });

                tvQuestionOption[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (q.iUID == QQData.m_iUID || q.iVoted == 1 || q.iLocked == 1)
                            return;

                        doVote(q, optidx);
                    }
                });

                tvQuestionOptionPercent[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (q.iUID == QQData.m_iUID || q.iVoted == 1 || q.iLocked == 1)
                            return;

                        doVote(q, optidx);
                    }
                });
            }

            if (q.iVoted == 1)
                ivQuestionSelectedIcon[q.iVoteNum].setVisibility(View.VISIBLE);
        }

        if (q.iUID != QQData.m_iUID) {
            if (q.iFaved == 1)
                ibFavIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_fav_red));

            ibFavIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleQuestionFav(q);
                }
            });

            if (q.iReported == 1) {
                ibQuestionReportTopbar.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_report_topbar_sent));
                ibQuestionReportTopbar.setClickable(false);
            } else {
                ibQuestionReportTopbar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (q.iReported == 0)
                            reportQuestion(q);
                    }
                });
            }
        }

        tvFavCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (q.iFavCount > 0) {
                    Bundle b = new Bundle();
                    b.putInt("qid", q.iQID);
                    Intent it = new Intent(QuestionActivity.this, FavListActivity.class);
                    it.putExtras(b);
                    Bundle translate = ActivityOptions.makeCustomAnimation(QuestionActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                    startActivity(it, translate);
                }
            }
        });

        tvReqCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (q.iRequestCount > 0) {
                    Bundle b = new Bundle();
                    b.putInt("qid", q.iQID);
                    Intent it = new Intent(QuestionActivity.this, RepostListActivity.class);
                    it.putExtras(b);
                    Bundle translate = ActivityOptions.makeCustomAnimation(QuestionActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                    startActivity(it, translate);
                }
            }
        });

        if (q.iUID == QQData.m_iUID) {
            ibQuestionRemoveTopbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeQuestion(q);
                }
            });
        } else {
            ibQuestionRemoveTopbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeRepost(q);
                }
            });
        }

        if (q.iUID != QQData.m_iUID) {
            ibReqIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (q.iReposted == 0)
                        repostQuestion(q);
                }
            });

            if (q.iReposted == 1) {
                ibReqIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_request_active));
                ibReqIcon.setClickable(false);
            }
        }

        tvFavCount.setText(String.valueOf(q.iFavCount));
        tvReqCount.setText(String.valueOf(q.iRequestCount));
        tvCommentCount.setText(String.valueOf(q.iCommentCount));
        ibCommentIcon.setClickable(false);

        q.cDesign.llItem = qItem;
        q.cDesign.civPhoto = civPhoto;
        q.cDesign.tvVoteCount = tvVoteCount;
        q.cDesign.ibFavIcon = ibFavIcon;
        q.cDesign.ibReqIcon = ibReqIcon;
        q.cDesign.ibExpandIcon = ibExpandIcon;
        q.cDesign.rlExpandIcon = rlExpandIcon;
        q.cDesign.tvFavCount = tvFavCount;
        q.cDesign.tvReqCount = tvReqCount;
        q.cDesign.llQuestionOptions = llQuestionOptions;
        q.cDesign.llQuestionOption = llQuestionOption;
        q.cDesign.tvQuestionOptionPercent = tvQuestionOptionPercent;
        q.cDesign.ivQuestionSelectedIcon = ivQuestionSelectedIcon;
        q.cDesign.civOptionImages = civOptionImages;

        if (q.iOptionCount > 0) {
            if (q.iUID == QQData.m_iUID || q.iVoted == 1 || q.iLocked == 1)
                showResults(q);
        }

        llQuestionBody.addView(q.cDesign.llItem);
    }

    public void doVote(final Question q, final int iOption) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(q.iQID))
                .add("opt", String.valueOf(iOption));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sVoteAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_DoVote(httpResponse, q, iOption);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_DoVote(HttpResponse httpResponse, final Question q, int iOption) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 641) {   // voted
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    q.iVoted = 1;
                    q.iVoteNum = iOption;

                    JSONArray jsonVotes = new JSONArray(jsonRet.getString("votes"));
                    getVoteResult(q, jsonVotes, iOption);
                    QQData.publishVoteResultAll(q.iQID, jsonVotes, iOption);
                } else if (iretval == 640) {   //already voted
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void getVoteResult(final Question q, JSONArray jsonVotes, int iOption) {
        try {
            Integer[] votes = null;
            if (q.iOptionCount > 0) {
                votes = new Integer[q.iOptionCount];
                for (int k = 0; k < q.iOptionCount; k++) {
                    votes[k] = jsonVotes.getInt(k + 1);
                }

                q.iVotes = votes;
                q.calculateTotalVotes();
                refreshVotes(q, iOption);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshVotes(final Question q, int iOption) {
        for (int i = 0; i < q.iOptionCount; i++) {
            q.cDesign.tvQuestionOptionPercent[i].setText("%" + String.valueOf(QQData.getVotePercent(q.iTotalVotes, q.iVotes[i])));
            q.cDesign.tvQuestionOptionPercent[i].setVisibility(View.VISIBLE);
        }

        q.cDesign.tvVoteCount.setText(String.valueOf(q.iTotalVotes));
        q.cDesign.ivQuestionSelectedIcon[iOption].setVisibility(View.VISIBLE);

        showResults(q);
    }

    public void toggleQuestionFav(final Question q) {
        if (q.iFaved == 0) {
            q.iFaved = 1;
            q.cDesign.ibFavIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_fav_red));
            q.iFavCount++;
            q.cDesign.tvFavCount.setText(String.valueOf(q.iFavCount));

            QQData.bangEffect(q.cDesign.ibFavIcon, QuestionActivity.this, Color.parseColor("#e74c3c"), Color.parseColor("#e74c3c"), null);
            sendQuestionFav(q);
            QQData.publishQuestionFavAll(q.iQID);
        } else {
            q.iFaved = 0;
            q.cDesign.ibFavIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_fav_selector));
            q.iFavCount--;
            q.cDesign.tvFavCount.setText(String.valueOf(q.iFavCount));
            sendQuestionUnfav(q);
            QQData.publishQuestionUnFavAll(q.iQID);
        }
    }

    public void sendQuestionFav(final Question q) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(q.iQID))
                .add("state", "1");

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sFavQuestionAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_FavQuestion(httpResponse, q);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_FavQuestion(HttpResponse httpResponse, final Question q) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 611) {   // fav process
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void sendQuestionUnfav(final Question q) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(q.iQID))
                .add("state", "0");

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sFavQuestionAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_UnfavQuestion(httpResponse, q);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_UnfavQuestion(HttpResponse httpResponse, final Question q) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 611) {   // fav process
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void reportQuestion(final Question q) {
        QQData.showConfirm(QuestionActivity.this, "Soruyu şikayet etmek istiyor musunuz?", new ActionClickListener() {
            @Override
            public void onActionClicked(Snackbar snackbar) {
                sendReportQuestion(q);
            }
        });
    }

    public void sendReportQuestion(final Question q) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(q.iQID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sReportQuestionAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_ReportQuestion(httpResponse, q);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        q.iReported = 1;
        ibQuestionReportTopbar.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_report_topbar_sent));
        ibQuestionReportTopbar.setClickable(false);

        QQData.pushPostRequest(pr);
    }

    public void getResult_ReportQuestion(HttpResponse httpResponse, final Question q) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 621) {   // reported
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void removeQuestion(final Question q) {
        QQData.showConfirm(QuestionActivity.this, "Soruyu silmek istiyor musunuz?", new ActionClickListener() {
            @Override
            public void onActionClicked(Snackbar snackbar) {
                sendRemoveQuestion(q);
            }
        });
    }

    public void sendRemoveQuestion(final Question q) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(q.iQID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sRemoveQuestionAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_RemoveQuestion(httpResponse, q);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
        QQData.publishRemoveQuestionAll(q.iQID);
        finish();
    }

    public void getResult_RemoveQuestion(HttpResponse httpResponse, final Question q) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 601) {   // question deleted
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void removeRepost(final Question q) {
        QQData.showConfirm(QuestionActivity.this, "Tekrar paylaşmayı iptal etmek istiyor musunuz?", new ActionClickListener() {
            @Override
            public void onActionClicked(Snackbar snackbar) {
                sendRemoveRepost(q);
            }
        });
    }

    public void sendRemoveRepost(final Question q) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(q.iQID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sRemoveRepostAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_RemoveRepost(httpResponse, q);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);

        ibQuestionRemoveTopbar.setVisibility(View.GONE);
        ibQuestionReportTopbar.setVisibility(View.VISIBLE);

        q.iReposted = 0;
        q.cDesign.ibReqIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_request_selector));
        q.iRequestCount--;
        q.cDesign.tvReqCount.setText(String.valueOf(q.iRequestCount));
        q.cDesign.ibReqIcon.setClickable(true);

        QQData.publishUnRepostQuestionAll(q.iQID);
    }

    public void getResult_RemoveRepost(HttpResponse httpResponse, final Question q) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 691) {   // question deleted
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public ArrayList<Integer> getSortedVoteList(final Question q) {
        Integer[] votes = q.iVotes.clone();
        ArrayList<Integer> sortedList = new ArrayList<Integer>();

        int val = 0, idx = 0;

        for (int i = 0; i < q.iOptionCount; i++) {
            val = votes[i];
            idx = i;
            for (int k = 0; k < q.iOptionCount; k++) {
                if (votes[k] > val) {
                    val = votes[k];
                    idx = k;
                }
            }
            votes[idx] = -1;
            sortedList.add(idx);
        }

        return sortedList;
    }

    public void showResults(final Question q) {

        if (q.iTotalVotes == 0)
            return;

        ArrayList<Integer> sortedList = getSortedVoteList(q);
        Integer[] bgList = {R.drawable.bg_question_option8, R.drawable.bg_question_option7, R.drawable.bg_question_option6, R.drawable.bg_question_option5, R.drawable.bg_question_option4, R.drawable.bg_question_option3, R.drawable.bg_question_option2, R.drawable.bg_question_option1};

        q.cDesign.llQuestionOptions.removeAllViews();

        for (int i = 0; i < sortedList.size(); i++) {
            q.cDesign.llQuestionOption[sortedList.get(i)].setBackgroundDrawable(getResources().getDrawable(bgList[i]));
            q.cDesign.llQuestionOptions.addView(q.cDesign.llQuestionOption[sortedList.get(i)]);

            if (q.cDesign.rlExpandIcon.getVisibility() == View.VISIBLE) {
                if (i < 4)
                    q.cDesign.llQuestionOption[sortedList.get(i)].setVisibility(View.VISIBLE);
                else
                    q.cDesign.llQuestionOption[sortedList.get(i)].setVisibility(View.GONE);
            }
        }

        if (q.cDesign.rlExpandIcon.getVisibility() == View.VISIBLE) {
            q.cDesign.ibExpandIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int i = 0; i < q.iOptionCount; i++)
                        if (q.cDesign.llQuestionOption[i].getVisibility() != View.VISIBLE)
                            q.cDesign.llQuestionOption[i].setVisibility(View.VISIBLE);
                    q.cDesign.rlExpandIcon.setVisibility(View.GONE);
                }
            });
        }

        q.cDesign.tvQuestionOptionPercent[sortedList.get(0)].setTypeface(q.cDesign.tvQuestionOptionPercent[sortedList.get(0)].getTypeface(), Typeface.BOLD);
    }

    public void sendComment(View v) {

        String comment = edComment.getText().toString();

        if (mQ == null || comment.length() == 0)
            return;

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(iQID))
                .add("votenum", String.valueOf(mQ.iVoteNum))
                .add("comment", comment);

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sSendCommentAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_SendComment(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
        edComment.setText("");
    }

    public void getResult_SendComment(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 665) {   // comment sent
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    JSONObject jsonComment = new JSONObject(jsonRet.getString("comment"));
                    addMyComment(jsonComment);
                } else if (iretval == 666) {   // question removed
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    QQData.publishRemoveQuestionAll(mQ.iQID);
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void addMyComment(JSONObject jsonComment) {
        CommentItem cit = null;
        try {
            cit = new CommentItem(
                    Integer.parseInt(jsonComment.getString("cid")),
                    QQData.m_iUID,
                    QQData.MyInfo.sNick,
                    jsonComment.getString("comment"),
                    QQData.m_sPPAdr,
                    jsonComment.getString("date")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinearLayout commentItem = (LinearLayout) getLayoutInflater().inflate(R.layout.comment_item, null);

        LinearLayout LL1 = (LinearLayout) commentItem.getChildAt(0);
        final de.hdodenhof.circleimageview.CircleImageView civPhoto = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(0);

        LinearLayout LL2 = (LinearLayout) LL1.getChildAt(1);
        TextView tvNick = (TextView) LL2.getChildAt(0);
        TextView tvComment = (TextView) LL2.getChildAt(1);

        LinearLayout LL3 = (LinearLayout) LL1.getChildAt(2);
        ImageButton ibRemove = (ImageButton) LL3.getChildAt(0);

        ibRemove.setVisibility(View.VISIBLE);
        final CommentItem fcit = cit;
        ibRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeComment(cList.get(fcit.iCID));
            }
        });

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

        tvNick.setText("@" + QQData.MyInfo.sNick + " | " + QQData.getDateDiff(cit.sDate));
        tvComment.setText(spanTaggedUsers(cit.sComment), TextView.BufferType.SPANNABLE);
        tvComment.setMovementMethod(LinkMovementMethod.getInstance());

        cit.cDesign.llItem = commentItem;
        cList.put(cit.iCID, cit);

        llQuestionComments.addView(cit.cDesign.llItem, 0);
    }

    public void getComments() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(iQID))
                .add("p", String.valueOf(iScrollLoadPage));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sGetCommentsAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (srlQuestion.isRefreshing())
                    srlQuestion.setRefreshing(false);

                getResult_GetComments(httpResponse);

                bScrollLoadProcess = false;
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

                if (srlQuestion.isRefreshing())
                    srlQuestion.setRefreshing(false);

                bScrollLoadProcess = false;
                iScrollLoadPage--;

                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_GetComments(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 661) {   // get comments
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));

                    if (!jsonRet.getString("list").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("list"));
                        getList(jsonList);
                    } else
                        iScrollLoadPage--;

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            iScrollLoadPage--;
            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void getList(JSONArray jsonList) {
        try {

            int cCount = jsonList.length();

            for (int i = 0; i < cCount; i++) {

                JSONObject jsonComment = jsonList.getJSONObject(i);
                int cid = Integer.parseInt(jsonComment.getString("cid"));
                int uid = Integer.parseInt(jsonComment.getString("uid"));
                int qid = Integer.parseInt(jsonComment.getString("qid"));
                int votenum = Integer.parseInt(jsonComment.getString("votenum"));
                String comment = jsonComment.getString("comment");
                String nick = jsonComment.getString("nick");
                String pp = jsonComment.getString("pp");
                String date = jsonComment.getString("date");

                CommentItem cit = new CommentItem(cid, uid, nick, comment, pp, date);
                addComment(cit);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addComment(final CommentItem cit) {
        LinearLayout commentItem = (LinearLayout) getLayoutInflater().inflate(R.layout.comment_item, null);

        LinearLayout LL1 = (LinearLayout) commentItem.getChildAt(0);
        final de.hdodenhof.circleimageview.CircleImageView civPhoto = (de.hdodenhof.circleimageview.CircleImageView) LL1.getChildAt(0);

        LinearLayout LL2 = (LinearLayout) LL1.getChildAt(1);
        TextView tvNick = (TextView) LL2.getChildAt(0);
        TextView tvComment = (TextView) LL2.getChildAt(1);

        LinearLayout LL3 = (LinearLayout) LL1.getChildAt(2);
        ImageButton ibRemove = (ImageButton) LL3.getChildAt(0);

        if (cit.iUID == QQData.m_iUID) {
            ibRemove.setVisibility(View.VISIBLE);
            ibRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeComment(cList.get(cit.iCID));
                }
            });
        }

        if (!cit.sPPName.equals("-1")) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.getThumbName(cit.sPPName), new NonViewAware(new ImageSize(64, 64), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    civPhoto.setImageBitmap(loadedImage);
                }
            });
        }

        civPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cit.iUID == QQData.m_iUID) {
                    QQData.clearAllActivities();
                    QQData.m_actHome.setFragment(4);
                    return;
                }

                Bundle b = new Bundle();
                b.putInt("target_uid", cit.iUID);
                Intent it = new Intent(QuestionActivity.this, UserProfileActivity.class);
                it.putExtras(b);
                Bundle translate = ActivityOptions.makeCustomAnimation(QuestionActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                startActivity(it, translate);
            }
        });

        tvNick.setText("@" + cit.sNick + " | " + QQData.getDateDiff(cit.sDate));
        tvComment.setText(spanTaggedUsers(cit.sComment), TextView.BufferType.SPANNABLE);
        tvComment.setMovementMethod(LinkMovementMethod.getInstance());

        cit.cDesign.llItem = commentItem;
        cList.put(cit.iCID, cit);

        llQuestionComments.addView(cit.cDesign.llItem);
    }

    public void removeComment(final CommentItem cit) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("cid", String.valueOf(cit.iCID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sRemoveCommentAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_RemoveComment(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
        cList.remove(cit.iCID);
        llQuestionComments.removeView(cit.cDesign.llItem);
    }

    public void getResult_RemoveComment(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 667) {   // comment deleted
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void repostQuestion(final Question q) {
        QQData.showConfirm(QuestionActivity.this, "Soruyu tekrar paylaşmak istiyor musunuz?", new ActionClickListener() {
            @Override
            public void onActionClicked(Snackbar snackbar) {
                sendRepostQuestion(q);
            }
        });
    }

    public void sendRepostQuestion(final Question q) {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("qid", String.valueOf(q.iQID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sRepostQuestionAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_RepostQuestion(httpResponse, q);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);

        q.iReposted = 1;
        q.cDesign.ibReqIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_request_active));
        q.iRequestCount++;
        q.cDesign.tvReqCount.setText(String.valueOf(q.iRequestCount));
        q.cDesign.ibReqIcon.setClickable(false);

        ibQuestionRemoveTopbar.setVisibility(View.VISIBLE);
        ibQuestionReportTopbar.setVisibility(View.GONE);

        QQData.publishRepostQuestionAll(q.iQID);

        Question cq1 = new Question(q);
        cq1.iReposterID = QQData.m_iUID;
        cq1.sReposterName = QQData.MyInfo.sName;
        QQData.m_actHome.fMyProfile.f_addQuestion(cq1, 0);

        Question cq2 = new Question(q);
        cq2.iReposterID = QQData.m_iUID;
        cq2.sReposterName = QQData.MyInfo.sName;
        QQData.m_actHome.fHome.fHomeFollowing.addQuestion(cq2, 0);
    }

    public void getResult_RepostQuestion(HttpResponse httpResponse, final Question q) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 671) {   // fav process
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void receiveVoteResult(int qid, JSONArray jsonVotes, int iOption) {
        if (mQ.iQID == qid) {
            if (mQ.iVoted == 0) {
                mQ.iVoted = 1;
                getVoteResult(mQ, jsonVotes, iOption);
            }
        }
    }

    public void receiveQuestionFav(int qid) {
        if (mQ.iQID == qid) {
            if (mQ.iFaved == 0) {
                mQ.iFaved = 1;
                mQ.cDesign.ibFavIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_fav_red));
                mQ.iFavCount++;
                mQ.cDesign.tvFavCount.setText(String.valueOf(mQ.iFavCount));
            }
        }
    }

    public void receiveQuestionUnFav(int qid) {
        if (mQ.iQID == qid) {
            if (mQ.iFaved == 1) {
                mQ.iFaved = 0;
                mQ.cDesign.ibFavIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_fav_selector));
                mQ.iFavCount--;
                mQ.cDesign.tvFavCount.setText(String.valueOf(mQ.iFavCount));
            }
        }
    }

    public void receiveRepostQuestion(int qid) {
        if (mQ.iQID == qid) {
            if (mQ.iReposted == 0) {
                mQ.iReposted = 1;
                mQ.cDesign.ibReqIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_request_active));
                mQ.iRequestCount++;
                mQ.cDesign.tvReqCount.setText(String.valueOf(mQ.iRequestCount));
                mQ.cDesign.ibReqIcon.setClickable(false);

                ibQuestionRemoveTopbar.setVisibility(View.VISIBLE);
                ibQuestionReportTopbar.setVisibility(View.GONE);
            }
        }
    }

    public void receiveUnRepostQuestion(int qid) {
        if (mQ.iQID == qid) {
            if (mQ.iReposted == 1) {
                mQ.iReposted = 0;
                mQ.cDesign.ibReqIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_question_request_selector));
                mQ.iRequestCount--;
                mQ.cDesign.tvReqCount.setText(String.valueOf(mQ.iRequestCount));
                mQ.cDesign.ibReqIcon.setClickable(true);

                ibQuestionRemoveTopbar.setVisibility(View.GONE);
                ibQuestionReportTopbar.setVisibility(View.VISIBLE);
            }
        }
    }

    public void receiveRemoveQuestion(int qid) {
        if (mQ.iQID == qid) {
            finish();
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
                        llPostCommentPredict.setVisibility(View.GONE);
                    } else {
                        cdt.cancel();
                        sSearchQuery = sSearchQuery.concat(String.valueOf(s.charAt(s.length() - 1)));
                        cdt.start();
                        llPostCommentPredict.setVisibility(View.VISIBLE);
                    }

                } else {    // silerkene

                    if (s.charAt(s.length() - 1) == ' ') {
                        sSearchQuery = "";
                        bConcatQuery = false;
                        llPostCommentPredict.setVisibility(View.GONE);
                    } else {
                        if (s.charAt(s.length() - 1) == '@') {
                            sSearchQuery = "";
                        } else {
                            cdt.cancel();
                            sSearchQuery = sSearchQuery.substring(0, sSearchQuery.length() - 1);
                            cdt.start();
                            llPostCommentPredict.setVisibility(View.VISIBLE);
                        }
                    }

                }
            }

        } else {
            sSearchQuery = "";
            bConcatQuery = false;
            llPostCommentPredict.setVisibility(View.GONE);
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
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
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
                    llPostCommentPredict.removeAllViews();

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
                QQData.showError(QuestionActivity.this, "İnternet bağlantısı kurulamadı");
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

        LinearLayout userItem = (LinearLayout) getLayoutInflater().inflate(R.layout.user_list_item, null);

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


        llPostCommentPredict.addView(userItem);
    }

    public void putPredictedNick(String nick) {
        String q = edComment.getText().toString();

        int prefix = q.lastIndexOf('@');
        if (prefix != -1) {
            q = q.substring(0, prefix);
            q = q.concat("@" + nick);
            edComment.setText(q);
            edComment.setSelection(q.length());
        }
        sSearchQuery = "";
        bConcatQuery = false;
        llPostCommentPredict.setVisibility(View.GONE);
    }

    public SpannableString spanTaggedUsers(String q) {
        int spos = 0, epos = 0;
        SpannableString ssQuestion = new SpannableString(q);

        while (true) {
            spos = q.indexOf('@', spos);
            if (spos != -1) {
                epos = q.indexOf(' ', spos);
                if (epos != -1) {

                    final String nick = q.substring(spos + 1, epos);
                    ssQuestion.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            Bundle b = new Bundle();
                            b.putString("nick", nick);
                            Intent it = new Intent(QuestionActivity.this, UserProfileActivity.class);
                            it.putExtras(b);
                            Bundle translate = ActivityOptions.makeCustomAnimation(QuestionActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                            startActivity(it, translate);
                        }

                        @Override
                        public void updateDrawState(final TextPaint textPaint) {
                            textPaint.setColor(Color.parseColor("#58ACFA"));
                            textPaint.setUnderlineText(false);
                            textPaint.setTypeface(Typeface.create(textPaint.getTypeface(), Typeface.BOLD));
                        }
                    }, spos, epos, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                } else {

                    final String nick = q.substring(spos + 1, q.length());
                    ssQuestion.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            Bundle b = new Bundle();
                            b.putString("nick", nick);
                            Intent it = new Intent(QuestionActivity.this, UserProfileActivity.class);
                            it.putExtras(b);
                            Bundle translate = ActivityOptions.makeCustomAnimation(QuestionActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
                            startActivity(it, translate);
                        }

                        @Override
                        public void updateDrawState(final TextPaint textPaint) {
                            textPaint.setColor(Color.parseColor("#58ACFA"));
                            textPaint.setUnderlineText(false);
                            textPaint.setTypeface(Typeface.create(textPaint.getTypeface(), Typeface.BOLD));
                        }
                    }, spos, q.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                }
                spos++;
            } else
                break;
        }

        return ssQuestion;
    }

}
