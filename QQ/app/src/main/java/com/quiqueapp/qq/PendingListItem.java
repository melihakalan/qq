package com.quiqueapp.qq;

import android.widget.LinearLayout;

/**
 * Created by user on 21.12.2015.
 */
public class PendingListItem {

    public int iUID;
    public String sNick;
    public String sName;
    public String sPPName;
    public Design cDesign;

    public PendingListItem(int iUID, String sNick, String sName, String sPPName) {
        this.iUID = iUID;
        this.sNick = sNick;
        this.sName = sName;
        this.sPPName = sPPName;
        this.cDesign = new Design();
    }

    class Design {
        public LinearLayout llItem;

        public Design() {
            this.llItem = null;
        }
    }
}
