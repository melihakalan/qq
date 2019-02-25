package com.quiqueapp.qq;

import android.widget.LinearLayout;

/**
 * Created by user on 21.12.2015.
 */
public class CommentItem {

    public int iCID;
    public int iUID;
    public String sNick;
    public String sComment;

    public String sPPName;
    public String sDate;

    public Design cDesign;

    public CommentItem(int iCID, int iUID, String sNick, String sComment, String sPPName, String sDate) {
        this.iCID = iCID;
        this.iUID = iUID;
        this.sNick = sNick;
        this.sComment = sComment;
        this.sPPName = sPPName;
        this.sDate = sDate;
        this.cDesign = new Design();
    }

    class Design {
        public LinearLayout llItem;

        public Design() {
            this.llItem = null;
        }
    }
}
