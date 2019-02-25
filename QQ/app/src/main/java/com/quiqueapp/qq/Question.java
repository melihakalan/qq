package com.quiqueapp.qq;

import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by user on 20.12.2015.
 */
public class Question {

    public int iUID;
    public String sPP;
    public String sNick;
    public String sName;

    public int iQID;
    public String sQuestion;
    public String sTags;
    int iAnonym;
    int iPrivate;
    int iOptionCount;
    int iRequestCount;
    int iFavCount;
    int iLocked;
    int iPastHour;
    public String[] sOptions;
    public Integer[] iVotes;
    public String[] sImages;
    public String sQuestionImage;
    public int iFaved;
    public int iReported;
    public int iVoted;
    public int iVoteNum;
    public int iCommentCount;
    public String sDate;
    public int iReposted;
    public int iReposterID;
    public String sReposterName;
    public int iTotalVotes;

    public Design cDesign;

    class Design {
        public LinearLayout llItem;

        public de.hdodenhof.circleimageview.CircleImageView civPhoto;

        public TextView tvVoteCount;

        public ImageButton ibFavIcon;
        public ImageButton ibReqIcon;
        public ImageButton ibExpandIcon;
        public FrameLayout rlExpandIcon;

        public TextView tvFavCount;
        public TextView tvReqCount;

        public LinearLayout[] llQuestionOption;
        public LinearLayout llQuestionOptions;
        public de.hdodenhof.circleimageview.CircleImageView[] civOptionImages;
        public TextView[] tvQuestionOptionPercent;
        public ImageView[] ivQuestionSelectedIcon;

        public Design() {
            this.llItem = null;
            this.civPhoto = null;
            this.tvVoteCount = null;
            this.ibFavIcon = null;
            this.ibReqIcon = null;
            this.ibExpandIcon = null;
            this.rlExpandIcon = null;
            this.tvFavCount = null;
            this.tvReqCount = null;
            this.llQuestionOption = null;
            this.llQuestionOptions = null;
            this.civOptionImages = null;
            this.tvQuestionOptionPercent = null;
            this.ivQuestionSelectedIcon = null;
        }
    }

    public Question(int iUID, String sPP, String sNick, String sName, int iQID, String sQuestion, String sTags, int iAnonym, int iPrivate, int iOptionCount, int iRequestCount, int iFavCount, int iLocked, int iPastHour, String[] sOptions, Integer[] iVotes, String[] sImages, String sQuestionImage, int iFaved, int iReported, int iVoted, int iCommentCount, String sDate, int iVoteNum, int iReposted, int iReposterID, String sReposterName) {
        this.iUID = iUID;
        this.sPP = sPP;
        this.sNick = sNick;
        this.sName = sName;
        this.iQID = iQID;
        this.sQuestion = sQuestion;
        this.sTags = sTags;
        this.iAnonym = iAnonym;
        this.iPrivate = iPrivate;
        this.iOptionCount = iOptionCount;
        this.iRequestCount = iRequestCount;
        this.iFavCount = iFavCount;
        this.iLocked = iLocked;
        this.iPastHour = iPastHour;
        this.sOptions = sOptions;
        this.iVotes = iVotes;
        this.sImages = sImages;
        this.sQuestionImage = sQuestionImage;
        this.iFaved = iFaved;
        this.iReported = iReported;
        this.iVoted = iVoted;
        this.iVoteNum = iVoteNum;
        this.iCommentCount = iCommentCount;
        this.sDate = sDate;
        this.iReposted = iReposted;
        this.iReposterID = iReposterID;
        this.sReposterName = sReposterName;
        this.iTotalVotes = 0;

        calculateTotalVotes();
        resetDesign();
    }

    public Question(Question q) {
        this.iUID = q.iUID;
        this.sPP = q.sPP;
        this.sNick = q.sNick;
        this.sName = q.sName;
        this.iQID = q.iQID;
        this.sQuestion = q.sQuestion;
        this.sTags = q.sTags;
        this.iAnonym = q.iAnonym;
        this.iPrivate = q.iPrivate;
        this.iOptionCount = q.iOptionCount;
        this.iRequestCount = q.iRequestCount;
        this.iFavCount = q.iFavCount;
        this.iLocked = q.iLocked;
        this.iPastHour = q.iPastHour;
        if (q.sOptions != null)
            this.sOptions = q.sOptions.clone();
        else
            this.sOptions = null;
        if (q.iVotes != null)
            this.iVotes = q.iVotes.clone();
        else
            this.iVotes = null;
        if (q.sImages != null)
            this.sImages = q.sImages.clone();
        else
            this.sImages = null;
        this.sQuestionImage = q.sQuestionImage;
        this.iFaved = q.iFaved;
        this.iReported = q.iReported;
        this.iVoted = q.iVoted;
        this.iVoteNum = q.iVoteNum;
        this.iCommentCount = q.iCommentCount;
        this.sDate = q.sDate;
        this.iReposted = q.iReposted;
        this.iReposterID = q.iReposterID;
        this.sReposterName = q.sReposterName;
        this.iTotalVotes = 0;

        calculateTotalVotes();
        resetDesign();
    }

    public void resetDesign() {
        this.cDesign = new Design();
        this.cDesign.llQuestionOption = new LinearLayout[iOptionCount];
        this.cDesign.civOptionImages = new CircleImageView[iOptionCount];
        this.cDesign.tvQuestionOptionPercent = new TextView[iOptionCount];
        this.cDesign.ivQuestionSelectedIcon = new ImageView[iOptionCount];
    }

    public void calculateTotalVotes() {
        if (this.iOptionCount > 0) {
            this.iTotalVotes = 0;
            for (int i = 0; i < this.iOptionCount; i++) {
                this.iTotalVotes += this.iVotes[i];
            }
        }
    }
}
