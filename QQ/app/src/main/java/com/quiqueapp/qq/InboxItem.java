package com.quiqueapp.qq;

import android.widget.LinearLayout;

/**
 * Created by user on 21.12.2015.
 */
public class InboxItem {

    public int iCID;
    public int iUID;
    public String sNick;
    public String sName;

    public int iBlockedMe;

    public String sPPName;

    public int iMsgCount;

    public Design cDesign;

    public InboxItem(int iCID, int iUID, String sNick, String sName, int iBlockedMe, String sPPName, int iMsgCount) {
        this.iCID = iCID;
        this.iUID = iUID;
        this.sNick = sNick;
        this.sName = sName;
        this.iBlockedMe = iBlockedMe;
        this.sPPName = sPPName;
        this.iMsgCount = iMsgCount;
        this.cDesign = new Design();
    }

    class Design {
        public LinearLayout llItem;

        public Design() {
            this.llItem = null;
        }
    }
}
