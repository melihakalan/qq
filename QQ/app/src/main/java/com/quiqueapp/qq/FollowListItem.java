package com.quiqueapp.qq;

import android.widget.ImageButton;
import android.widget.LinearLayout;

/**
 * Created by user on 21.12.2015.
 */
public class FollowListItem {

    public int iUID;
    public String sNick;
    public String sName;

    public int iBlockedMe;
    public int iFollowState;

    public String sPPName;

    public Design cDesign;

    public FollowListItem(int iUID, String sNick, String sName, int iBlockedMe, int iFollowState, String sPPName) {
        this.iUID = iUID;
        this.sNick = sNick;
        this.sName = sName;
        this.iBlockedMe = iBlockedMe;
        this.iFollowState = iFollowState;
        this.sPPName = sPPName;
        this.cDesign = new Design();
    }

    class Design {
        public LinearLayout llItem;

        public ImageButton ibFollowProcess;

        public Design() {
            this.llItem = null;
            this.ibFollowProcess = null;
        }
    }
}
