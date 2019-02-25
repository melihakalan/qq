package com.quiqueapp.qq;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.Years;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import xyz.hanks.library.SmallBang;
import xyz.hanks.library.SmallBangListener;

/**
 * Created by user on 13.11.2015.
 */
public class QQData {
    public static String m_sServer = "http://quiqueapp411.com/qq";
    public static String m_sDataSPName = "qq_userdata";
    public static String m_sSPKey = "f6t5fa2ua7fbp77y1yx7";
    public static String m_sPPAdr = m_sServer + "/pp/";
    public static String m_sOptionImageAdr = m_sServer + "/opimg/";
    public static String m_sQuestionImageAdr = m_sServer + "/qimg/";

    public static int m_iUID = -1;
    public static int m_iSID = 0;

    public static boolean m_bFBAccount = false;
    public static boolean m_bNotifications = true;

    public static ArrayList<Activity> m_lActivityList;
    public static ArrayList<Activity> m_lVisibleActivityList;

    public static HomeActivity m_actHome = null;
    public static Context m_ctxAPP = null;
    public static Crypt m_cryptor = new Crypt();
    public static UserProfileActivity m_actLastUserProfile = null;
    public static boolean m_bInPostProcess = false;
    public static ArrayList<PostRequest> m_lPostList;
    public static Typeface tfRobotoCondensed = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    public static Typeface tfRobotoCondensedBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);

    public static String m_sCheckMailAdr = "/qq_checkmail";
    public static String m_sLoginAdr = "/qq_login";
    public static String m_sSignupAdr = "/qq_signup";
    public static String m_sSendQAdr = "/qq_sendquestion";
    public static String m_sFBLoginAdr = "/qq_loginfb";
    public static String m_sUpdateProfileAdr = "/qq_updateprofile";
    public static String m_sGetLaunchInfoAdr = "/qq_getlaunchinfo";
    public static String m_sGetUserProfileAdr = "/qq_getuserprofile";
    public static String m_sSaveUserStateAdr = "/qq_saveuserstate";
    public static String m_sFollowProcessAdr = "/qq_followprocess";
    public static String m_sReportAdr = "/qq_report";
    public static String m_sGetFollowListAdr = "/qq_getfollowlist";
    public static String m_sSavePrivateStateAdr = "/qq_saveprivatestate";
    public static String m_sChangePWAdr = "/qq_changepwadr";
    public static String m_sGetMyQuestionsAdr = "/qq_getmyquestions";
    public static String m_sGetUserQuestionsAdr = "/qq_getuserquestions";
    public static String m_sRemoveQuestionAdr = "/qq_removequestion";
    public static String m_sFavQuestionAdr = "/qq_favquestion";
    public static String m_sReportQuestionAdr = "/qq_reportquestion";
    public static String m_sUploadProfilePhotoAdr = "/qq_uploadprofilephoto";
    public static String m_sRemoveProfilePhotoAdr = "/qq_removeprofilephoto";
    public static String m_sGetFavListAdr = "/qq_getfavlist";
    public static String m_sVoteAdr = "/qq_vote";
    public static String m_sGetFollowingQuestionsAdr = "/qq_getfollowingquestions";
    public static String m_sGetTrendQuestionsAdr = "/qq_gettrendquestions";
    public static String m_sGetAllQuestionsAdr = "/qq_getallquestions";
    public static String m_sGetInboxAdr = "/qq_getinbox";
    public static String m_sGetMessagesAdr = "/qq_getmessages";
    public static String m_sGetMessagerInfoAdr = "/qq_getmessagerinfo";
    public static String m_sSendMessageAdr = "/qq_sendmessage";
    public static String m_sRemoveChatAdr = "/qq_removechat";
    public static String m_sGetQuestionAdr = "/qq_getquestion";
    public static String m_sSendCommentAdr = "/qq_sendcomment";
    public static String m_sGetCommentsAdr = "/qq_getcomments";
    public static String m_sRemoveCommentAdr = "/qq_removecomment";
    public static String m_sGetUserFavQuestionsAdr = "/qq_getuserfavquestions";
    public static String m_sGetLocationQuestionsAdr = "/qq_getlocationquestions";
    public static String m_sGetTrendingTagsAdr = "/qq_gettrendingtags";
    public static String m_sGetTagQuestionsAdr = "/qq_gettagquestions";
    public static String m_sSearchTagAdr = "/qq_searchtag";
    public static String m_sSearchNickAdr = "/qq_searchnick";
    public static String m_sSearchNameAdr = "/qq_searchname";
    public static String m_sUpdateNotificationsAdr = "/qq_updatenotifications";
    public static String m_sGetNotificationsAdr = "/qq_getnotifications";
    public static String m_sGetNewNotificationsAdr = "/qq_getnewnotifications";
    public static String m_sCheckNewNotificationsAdr = "/qq_checknewnotifications";
    public static String m_sUpdateMessagesCheckAdr = "/qq_updatemessagescheck";
    public static String m_sRepostQuestionAdr = "/qq_repostquestion";
    public static String m_sGetRepostListAdr = "/qq_getrepostlist";
    public static String m_sRemoveRepostAdr = "/qq_removerepost";
    public static String m_sGetPendingListAdr = "/qq_getpendinglist";
    public static String m_sAcceptPendingAdr = "/qq_acceptpending";
    public static String m_sRejectPendingAdr = "/qq_rejectpending";
    public static String m_sGetIdFromNickAdr = "/qq_getidfromnick";

    public static class MyInfo {
        public static String sNick = null;
        public static String sName = null;
        public static String sBio = null;
        public static String sPhone = null;
        public static int iGender = 0;
        public static int iQuestions = 0;
        public static int iQP = 0;
        public static int iFollowingCount = 0;
        public static int iFollowerCount = 0;
        public static boolean bPrivate = false;
        public static String sPPName = null;

        public static void initData() {
            sNick = null;
            sName = null;
            sBio = null;
            sPhone = null;
            iGender = 0;
            iQuestions = 0;
            iQP = 0;
            iFollowingCount = 0;
            iFollowerCount = 0;
            bPrivate = false;
            sPPName = null;
        }
    }

    public static void initData() {
        m_iUID = -1;
        m_iSID = 0;

        m_bFBAccount = false;
        m_bNotifications = true;

        m_lActivityList = new ArrayList<Activity>();
        m_lVisibleActivityList = new ArrayList<Activity>();

        m_actHome = null;
        m_ctxAPP = null;
        m_actLastUserProfile = null;
        m_bInPostProcess = false;
        m_lPostList = new ArrayList<PostRequest>();

        MyInfo.initData();
    }

    public static void initUIL() {
        try {
            DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .build();

            File cacheDir = StorageUtils.getCacheDirectory(m_ctxAPP);

            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(m_ctxAPP)
                    .defaultDisplayImageOptions(defaultOptions)
                    .memoryCache(new LruMemoryCache(3 * 1024 * 1024))
                    .diskCache(new LruDiskCache(cacheDir, null, new HashCodeFileNameGenerator(), 100 * 1024 * 1024, 5000))
                    .build();

            ImageLoader.getInstance().init(config);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: initUIL!");
        }
    }

    public static boolean isAlphaNumeric(String s) {
        return s.matches("^[a-zA-Z0-9_]*$");
    }

    public static boolean isJSON(String s) {
        try {
            new JSONObject(s);
        } catch (JSONException ex) {
            try {
                new JSONArray(s);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    public static void saveUserSID() {
        SecurePreferences SPFile = new SecurePreferences(m_ctxAPP, m_sDataSPName, m_sSPKey, true);
        SPFile.put("sid", String.valueOf(m_iSID));
        System.out.println("Saved SID: " + m_iSID);
    }

    public static void resetUserData() {
        SecurePreferences SPFile = new SecurePreferences(m_ctxAPP, m_sDataSPName, m_sSPKey, true);
        SPFile.put("email", null);
        SPFile.put("uid", null);
        SPFile.put("sid", null);
        SPFile.put("fbaccount", null);
        SPFile.put("notifications", null);
    }

    public static void saveUserNotifications() {
        SecurePreferences SPFile = new SecurePreferences(m_ctxAPP, m_sDataSPName, m_sSPKey, true);
        SPFile.put("notifications", String.valueOf((QQData.m_bNotifications) ? 1 : 0));
    }

    public static void clearAllActivities() {
        if (m_lActivityList.isEmpty())
            return;

        for (int i = 0; i < m_lActivityList.size(); i++) {
            Activity act = m_lActivityList.get(i);
            act.finish();
        }

        m_lActivityList.clear();
    }

    public static int getVotePercent(int iTotalVotes, int iVote) {
        if (iTotalVotes == 0 || iVote == 0)
            return 0;

        float fdiv = (float) iTotalVotes / (float) iVote;
        return (int) (100 / fdiv);
    }

    public static boolean inPostProcess() {
        return m_bInPostProcess;
    }

    public static void setPostProcess(boolean flag) {
        m_bInPostProcess = flag;
    }

    public static void clearPostRequests() {
        m_lPostList.clear();
        setPostProcess(false);
    }

    public static void pushPostRequest(final PostRequest pr) {

        m_lPostList.add(pr);

        if (!inPostProcess()) {
            setPostProcess(true);
            processPostRequest(pr);
        }
    }

    public static void processPostRequest(final PostRequest pr) {

        pr.client.post(pr.adr, pr.pm, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                if (m_lPostList.contains(pr)) {
                    pr.callback.onComplete(httpResponse);
                    saveUserSID();
                    m_lPostList.remove(pr);
                }

                if (!m_lPostList.isEmpty()) {
                    PostRequest prnext = m_lPostList.get(0);
                    if (prnext.pm.containsKey("sid"))
                        prnext.pm.put("sid", String.valueOf(m_iSID));
                    processPostRequest(prnext);
                } else {
                    setPostProcess(false);
                }
            }

            @Override
            public void onError(Exception e) {
                pr.callback.onError(e);
                clearPostRequests();
            }
        });
    }

    public static String getDateDiff(String date) {
        try {
            DateTime dtDate = new DateTime(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(date));
            DateTime dtNow = DateTime.now();

            if (Minutes.minutesBetween(dtDate, dtNow).getMinutes() == 0)
                return "şimdi";
            else if (Hours.hoursBetween(dtDate, dtNow).getHours() == 0)
                return Minutes.minutesBetween(dtDate, dtNow).getMinutes() + "dk";
            else if (Days.daysBetween(dtDate, dtNow).getDays() == 0)
                return Hours.hoursBetween(dtDate, dtNow).getHours() + "sa";
            else if (Months.monthsBetween(dtDate, dtNow).getMonths() == 0)
                return Days.daysBetween(dtDate, dtNow).getDays() + " gün";
            else if (Years.yearsBetween(dtDate, dtNow).getYears() == 0)
                return Months.monthsBetween(dtDate, dtNow).getMonths() + " ay";
            else
                return Years.yearsBetween(dtDate, dtNow).getYears() + " yıl";

        } catch (Exception e) {
            return "";
        }
    }

    public static void bangEffect(View v, Activity act, int c1, int c2, SmallBangListener sbl) {
        SmallBang sb = SmallBang.attach2Window(act);
        int[] colors = {c1, c2};
        sb.setColors(colors);
        if (sbl != null)
            sb.setmListener(sbl);
        sb.bang(v);
    }

    public static void bangEffect(View v, Activity act, int c1, int c2, SmallBangListener sbl, int radius) {
        SmallBang sb = SmallBang.attach2Window(act);
        int[] colors = {c1, c2};
        sb.setColors(colors);
        sb.bang(v, radius, sbl);
    }

    public static String getThumbName(String fname) {
        String[] parts = fname.split("\\.");

        String file = parts[0];
        file += "_thumb.jpg";
        return file;
    }

    public static void showError(Context ctx, String error) {
        SnackbarManager.show(
                Snackbar.with(ctx)
                        .text(error)
                        .textColor(Color.parseColor("#EEEEEE"))
                        .textTypeface(tfRobotoCondensed)
                        .color(Color.parseColor("#F0c0392b"))
                        .duration(3500)
                        .type(SnackbarType.MULTI_LINE)
        );
    }

    public static Snackbar showInfo(Context ctx, String info) {
        Snackbar sb = Snackbar.with(ctx)
                .text(info)
                .textColor(Color.parseColor("#EEEEEE"))
                .textTypeface(tfRobotoCondensed)
                .color(Color.parseColor("#F02980b9"))
                .duration(3500)
                .type(SnackbarType.MULTI_LINE);

        SnackbarManager.show(sb);
        return sb;
    }

    public static void showInfoButton(Context ctx, String info, String button, ActionClickListener acl) {
        SnackbarManager.show(
                Snackbar.with(ctx)
                        .text(info)
                        .textColor(Color.parseColor("#EEEEEE"))
                        .textTypeface(tfRobotoCondensed)
                        .color(Color.parseColor("#F02980b9"))
                        .duration(3500)
                        .actionLabel(button)
                        .actionColor(Color.parseColor("#EEEEEE"))
                        .actionLabelTypeface(tfRobotoCondensedBold)
                        .actionListener(acl)
                        .type(SnackbarType.MULTI_LINE)
        );
    }

    public static Snackbar showInfoInfinite(Context ctx, String info) {
        Snackbar sb = Snackbar.with(ctx)
                .text(info)
                .textColor(Color.parseColor("#EEEEEE"))
                .textTypeface(tfRobotoCondensed)
                .color(Color.parseColor("#F02980b9"))
                .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                .type(SnackbarType.MULTI_LINE);

        SnackbarManager.show(sb);
        return sb;
    }

    public static void showConfirm(Context ctx, String info, ActionClickListener acl) {
        SnackbarManager.show(
                Snackbar.with(ctx)
                        .text(info)
                        .textColor(Color.parseColor("#EEEEEE"))
                        .textTypeface(tfRobotoCondensed)
                        .color(Color.parseColor("#F0333333"))
                        .duration(3500)
                        .actionLabel("EVET")
                        .actionColor(Color.parseColor("#58ACFA"))
                        .actionLabelTypeface(tfRobotoCondensedBold)
                        .actionListener(acl)
                        .type(SnackbarType.MULTI_LINE)
        );
    }

    public static boolean isAppVisible() {
        return !(m_actHome.bActivityPaused && m_lVisibleActivityList.isEmpty());
    }

    public static Activity getTopActivity() {
        Activity topActivity = QQData.m_actHome;
        if (!m_lVisibleActivityList.isEmpty())
            topActivity = m_lVisibleActivityList.get(0);
        return topActivity;
    }

    public static void publishVoteResultAll(int qid, JSONArray jsonVotes, int iOption) {
        m_actHome.fHome.fHomeAll.receiveVoteResult(qid, jsonVotes, iOption);
        m_actHome.fHome.fHomeFollowing.receiveVoteResult(qid, jsonVotes, iOption);
        m_actHome.fHome.fHomeTrend.receiveVoteResult(qid, jsonVotes, iOption);
        m_actHome.fMyProfile.receiveVoteResult(qid, jsonVotes, iOption);

        if (!m_lActivityList.isEmpty()) {
            for (Activity act : m_lActivityList) {

                if (act instanceof FavQuestionsActivity)
                    ((FavQuestionsActivity) act).receiveVoteResult(qid, jsonVotes, iOption);
                else if (act instanceof LocationQuestionsActivity)
                    ((LocationQuestionsActivity) act).receiveVoteResult(qid, jsonVotes, iOption);
                else if (act instanceof QuestionActivity)
                    ((QuestionActivity) act).receiveVoteResult(qid, jsonVotes, iOption);
                else if (act instanceof TagSearchActivity)
                    ((TagSearchActivity) act).receiveVoteResult(qid, jsonVotes, iOption);
                else if (act instanceof UserProfileActivity)
                    ((UserProfileActivity) act).receiveVoteResult(qid, jsonVotes, iOption);

            }
        }
    }

    public static void publishQuestionFavAll(int qid) {
        m_actHome.fHome.fHomeAll.receiveQuestionFav(qid);
        m_actHome.fHome.fHomeFollowing.receiveQuestionFav(qid);
        m_actHome.fHome.fHomeTrend.receiveQuestionFav(qid);
        m_actHome.fMyProfile.receiveQuestionFav(qid);

        if (!m_lActivityList.isEmpty()) {
            for (Activity act : m_lActivityList) {

                if (act instanceof FavQuestionsActivity)
                    ((FavQuestionsActivity) act).receiveQuestionFav(qid);
                else if (act instanceof LocationQuestionsActivity)
                    ((LocationQuestionsActivity) act).receiveQuestionFav(qid);
                else if (act instanceof QuestionActivity)
                    ((QuestionActivity) act).receiveQuestionFav(qid);
                else if (act instanceof TagSearchActivity)
                    ((TagSearchActivity) act).receiveQuestionFav(qid);
                else if (act instanceof UserProfileActivity)
                    ((UserProfileActivity) act).receiveQuestionFav(qid);

            }
        }
    }

    public static void publishQuestionUnFavAll(int qid) {
        m_actHome.fHome.fHomeAll.receiveQuestionUnFav(qid);
        m_actHome.fHome.fHomeFollowing.receiveQuestionUnFav(qid);
        m_actHome.fHome.fHomeTrend.receiveQuestionUnFav(qid);
        m_actHome.fMyProfile.receiveQuestionUnFav(qid);

        if (!m_lActivityList.isEmpty()) {
            for (Activity act : m_lActivityList) {

                if (act instanceof FavQuestionsActivity)
                    ((FavQuestionsActivity) act).receiveQuestionUnFav(qid);
                else if (act instanceof LocationQuestionsActivity)
                    ((LocationQuestionsActivity) act).receiveQuestionUnFav(qid);
                else if (act instanceof QuestionActivity)
                    ((QuestionActivity) act).receiveQuestionUnFav(qid);
                else if (act instanceof TagSearchActivity)
                    ((TagSearchActivity) act).receiveQuestionUnFav(qid);
                else if (act instanceof UserProfileActivity)
                    ((UserProfileActivity) act).receiveQuestionUnFav(qid);

            }
        }
    }

    public static void publishRemoveQuestionAll(int qid) {
        m_actHome.fHome.fHomeAll.receiveRemoveQuestion(qid);
        m_actHome.fHome.fHomeFollowing.receiveRemoveQuestion(qid);
        m_actHome.fHome.fHomeTrend.receiveRemoveQuestion(qid);
        m_actHome.fMyProfile.receiveRemoveQuestion(qid);

        if (!m_lActivityList.isEmpty()) {
            for (Activity act : m_lActivityList) {

                if (act instanceof FavQuestionsActivity)
                    ((FavQuestionsActivity) act).receiveRemoveQuestion(qid);
                else if (act instanceof LocationQuestionsActivity)
                    ((LocationQuestionsActivity) act).receiveRemoveQuestion(qid);
                else if (act instanceof QuestionActivity)
                    ((QuestionActivity) act).receiveRemoveQuestion(qid);
                else if (act instanceof TagSearchActivity)
                    ((TagSearchActivity) act).receiveRemoveQuestion(qid);
                else if (act instanceof UserProfileActivity)
                    ((UserProfileActivity) act).receiveRemoveQuestion(qid);

            }
        }
    }

    public static void publishRepostQuestionAll(int qid) {
        m_actHome.fHome.fHomeAll.receiveRepostQuestion(qid);
        m_actHome.fHome.fHomeFollowing.receiveRepostQuestion(qid);
        m_actHome.fHome.fHomeTrend.receiveRepostQuestion(qid);
        m_actHome.fMyProfile.receiveRepostQuestion(qid);

        if (!m_lActivityList.isEmpty()) {
            for (Activity act : m_lActivityList) {

                if (act instanceof FavQuestionsActivity)
                    ((FavQuestionsActivity) act).receiveRepostQuestion(qid);
                else if (act instanceof LocationQuestionsActivity)
                    ((LocationQuestionsActivity) act).receiveRepostQuestion(qid);
                else if (act instanceof QuestionActivity)
                    ((QuestionActivity) act).receiveRepostQuestion(qid);
                else if (act instanceof TagSearchActivity)
                    ((TagSearchActivity) act).receiveRepostQuestion(qid);
                else if (act instanceof UserProfileActivity)
                    ((UserProfileActivity) act).receiveRepostQuestion(qid);

            }
        }
    }

    public static void publishUnRepostQuestionAll(int qid) {
        m_actHome.fHome.fHomeAll.receiveUnRepostQuestion(qid);
        m_actHome.fHome.fHomeFollowing.receiveUnRepostQuestion(qid);
        m_actHome.fHome.fHomeTrend.receiveUnRepostQuestion(qid);
        m_actHome.fMyProfile.receiveUnRepostQuestion(qid);

        if (!m_lActivityList.isEmpty()) {
            for (Activity act : m_lActivityList) {

                if (act instanceof FavQuestionsActivity)
                    ((FavQuestionsActivity) act).receiveUnRepostQuestion(qid);
                else if (act instanceof LocationQuestionsActivity)
                    ((LocationQuestionsActivity) act).receiveUnRepostQuestion(qid);
                else if (act instanceof QuestionActivity)
                    ((QuestionActivity) act).receiveUnRepostQuestion(qid);
                else if (act instanceof TagSearchActivity)
                    ((TagSearchActivity) act).receiveUnRepostQuestion(qid);
                else if (act instanceof UserProfileActivity)
                    ((UserProfileActivity) act).receiveUnRepostQuestion(qid);

            }
        }
    }

}
