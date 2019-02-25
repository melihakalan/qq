package com.quiqueapp.qq;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Bundle;

import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Random;

public class NotificationService extends IntentService {

    private String m_sServer = "http://melihakalan.com/qq";
    private String m_sPPAdr = m_sServer + "/pp/";
    private String m_sCheckNewNotificationsAdr = "/qq_checknewnotifications";
    private String m_sGetNewNotificationsAdr = "/qq_getnewnotifications";

    private Context ctxAPP = null;
    private int mUID = -1;
    private int nNotificationId = 0;

    public NotificationService() {
        super("qqns");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ctxAPP = getApplicationContext();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle b = intent.getExtras();
        mUID = b.getInt("uid");

        nNotificationId = new Random().nextInt(1000000);

        serviceFunction();
    }

    public void serviceFunction() {
        System.out.println("Service: function: mUID: " + mUID + " n: " + nNotificationId);

        if (checkNewNotifications() == true) {
            getNewNotifications();
        }
    }

    public boolean checkNewNotifications() {
        AndroidHttpClient httpClient = new AndroidHttpClient(m_sServer);
        httpClient.setConnectionTimeout(5000);
        httpClient.setMaxRetries(1);
        ParameterMap postparams = httpClient.newParams()
                .add("uid", String.valueOf(mUID));
        HttpResponse httpResponse = httpClient.post(m_sCheckNewNotificationsAdr, postparams);

        if (httpResponse == null)
            return false;

        return httpResponse.getBodyAsString().equals("1");
    }

    public void getNewNotifications() {
        AndroidHttpClient httpClient = new AndroidHttpClient(m_sServer);
        httpClient.setConnectionTimeout(5000);
        httpClient.setMaxRetries(1);
        ParameterMap postparams = httpClient.newParams()
                .add("uid", String.valueOf(mUID));

        HttpResponse httpResponse = httpClient.post(m_sGetNewNotificationsAdr, postparams);

        if (httpResponse != null) {
            fetchNewNotifications(httpResponse);
        }
    }

    public void fetchNewNotifications(HttpResponse httpResponse) {
        if (!ImageLoader.getInstance().isInited())
            initUIL();

        String sRet = httpResponse.getBodyAsString();
        if (isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 820) {   // new notifications
                    if (!jsonRet.getString("list").equals("0")) {
                        JSONArray jsonList = new JSONArray(jsonRet.getString("list"));
                        getNewList(jsonList);
                    }

                    int pending_count = Integer.parseInt(jsonRet.getString("pendingcount"));
                    if (pending_count > 0 && !jsonRet.getString("pendinglist").equals("0")) {
                        JSONArray jsonPendingList = new JSONArray(jsonRet.getString("pendinglist"));
                        getNewPendingList(jsonPendingList);
                    }

                    if (!jsonRet.getString("newmessages").equals("0")) {
                        JSONArray jsonNewMessages = new JSONArray(jsonRet.getString("newmessages"));
                        getNewMessages(jsonNewMessages);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void getNewList(JSONArray jsonList) {
        try {

            int count = jsonList.length();

            for (int i = 0; i < count; i++) {

                JSONObject jsonNotification = jsonList.getJSONObject(i);

                int uid = Integer.parseInt(jsonNotification.getString("uid"));
                int uid2 = Integer.parseInt(jsonNotification.getString("uid2"));
                int type = Integer.parseInt(jsonNotification.getString("type"));
                int param = -1;
                if (!jsonNotification.getString("param").equals("null"))
                    param = Integer.parseInt(jsonNotification.getString("param"));
                String date = jsonNotification.getString("date");
                String name = jsonNotification.getString("name");
                String pp = jsonNotification.getString("pp");

                pushNewNotifications(uid2, type, param, date, name, pp);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void pushNewNotifications(int uid, int type, int param, String date, String name, String pp) {
        Intent it = null;
        PendingIntent pi = null;
        Bundle b = new Bundle();

        switch (type) {
            case 10:
                it = new Intent(this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                b.putString("navigate", "user_profile");
                b.putInt("target_uid", uid);
                it.putExtras(b);
                pi = PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_CANCEL_CURRENT);
                pushNotification(pi, name, "seni takip etmeye başladı", pp);
                break;

            case 12:
                it = new Intent(this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                b.putString("navigate", "question");
                b.putInt("qid", param);
                it.putExtras(b);
                pi = PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_CANCEL_CURRENT);
                pushNotification(pi, name, "yeni bir gönderi paylaştı", pp);
                break;

            case 13:
                it = new Intent(this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                b.putString("navigate", "user_profile");
                b.putInt("target_uid", uid);
                it.putExtras(b);
                pi = PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_CANCEL_CURRENT);
                pushNotification(pi, name, "takip isteğini kabul etti", pp);
                break;

            case 20:
                it = new Intent(this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                b.putString("navigate", "question");
                b.putInt("qid", param);
                it.putExtras(b);
                pi = PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_CANCEL_CURRENT);
                pushNotification(pi, name, "gönderini beğendi", pp);
                break;

            case 21:
                it = new Intent(this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                b.putString("navigate", "question");
                b.putInt("qid", param);
                it.putExtras(b);
                pi = PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_CANCEL_CURRENT);
                pushNotification(pi, name, "gönderine bir yorum yaptı", pp);
                break;

            case 22:
                it = new Intent(this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                b.putString("navigate", "question");
                b.putInt("qid", param);
                it.putExtras(b);
                pi = PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_CANCEL_CURRENT);
                pushNotification(pi, name, "gönderini tekrar paylaştı", pp);
                break;

            case 23:
                it = new Intent(this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                b.putString("navigate", "question");
                b.putInt("qid", param);
                it.putExtras(b);
                pi = PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_CANCEL_CURRENT);
                pushNotification(pi, name, "bir gönderide senden bahsetti", pp);
                break;
        }
    }

    public void getNewPendingList(JSONArray jsonPendingList) {
        try {

            int count = jsonPendingList.length();

            for (int i = 0; i < count; i++) {

                JSONObject jsonPending = jsonPendingList.getJSONObject(i);
                int uid = Integer.parseInt(jsonPending.getString("uid"));
                String date = jsonPending.getString("date");
                String name = jsonPending.getString("name");
                String pp = jsonPending.getString("pp");

                Intent it = new Intent(this, LoginActivity.class);
                Bundle b = new Bundle();
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                b.putString("navigate", "pendings");
                b.putInt("target_uid", uid);
                it.putExtras(b);
                PendingIntent pi = PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_CANCEL_CURRENT);
                pushNotification(pi, name, "seni takip etmek istiyor", pp);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getNewMessages(JSONArray jsonNewMessages) {
        try {

            int count = jsonNewMessages.length();

            for (int i = 0; i < count; i++) {

                JSONObject jsonMessage = jsonNewMessages.getJSONObject(i);
                final int uid = Integer.parseInt(jsonMessage.getString("uid"));
                String date = jsonMessage.getString("date");
                String name = jsonMessage.getString("name");
                String pp = jsonMessage.getString("pp");

                Intent it = new Intent(this, LoginActivity.class);
                Bundle b = new Bundle();
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                b.putString("navigate", "messages");
                b.putInt("tid", uid);
                it.putExtras(b);
                PendingIntent pi = PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_CANCEL_CURRENT);
                pushNotification(pi, name, "sana bir mesaj gönderdi", pp);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void pushNotification(PendingIntent pi, String name, String msg, String pp) {
        Bitmap bmPP = ImageLoader.getInstance().loadImageSync(m_sPPAdr + getThumbName(pp), new ImageSize(128, 128));
        bmPP = getCroppedBitmap(bmPP);

        Notification n = new Notification.Builder(this)
                .setContentTitle(name)
                .setContentText(msg)
                .setContentIntent(pi)
                .setSmallIcon(R.drawable.logo_64)
                .setLargeIcon(bmPP)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true).build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(nNotificationId, n);
        nNotificationId++;
    }

    public boolean isJSON(String s) {
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

    public String getThumbName(String fname) {
        String[] parts = fname.split("\\.");

        String file = parts[0];
        file += "_thumb.jpg";
        return file;
    }

    public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output;
    }

    public void initUIL() {
        try {
            DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .build();

            File cacheDir = StorageUtils.getCacheDirectory(ctxAPP);

            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(ctxAPP)
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
}
