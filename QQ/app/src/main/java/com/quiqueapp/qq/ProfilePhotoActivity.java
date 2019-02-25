package com.quiqueapp.qq;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfilePhotoActivity extends Activity {

    private float fDragY = 0;
    private ImageView ivCurrentProfilePhoto;
    private Bitmap bmNewPhoto = null;
    private Snackbar sbUploading = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_photo);

        QQData.m_lActivityList.add(ProfilePhotoActivity.this);

        ivCurrentProfilePhoto = (ImageView) findViewById(R.id.ivCurrentProfilePhoto);

        ivCurrentProfilePhoto.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return dragClose(v, event);
            }
        });

        if (QQData.MyInfo.sPPName != null) {
            ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + QQData.MyInfo.sPPName, new NonViewAware(new ImageSize(320, 320), ViewScaleType.CROP), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    ivCurrentProfilePhoto.setImageBitmap(loadedImage);
                }
            });
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_fade_in, R.anim.anim_drag_down);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(ProfilePhotoActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(ProfilePhotoActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(ProfilePhotoActivity.this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case 100:    // SELECT_PHOTO
                if (resultCode == RESULT_OK) {
                    getNewPhoto(imageReturnedIntent.getData());
                }
                break;
        }
    }

    public void getNewPhoto(Uri uriImage) {
        Bitmap bmPhoto = null;

        try {
            bmPhoto = decodeSampledBitmap(uriImage, 320, 320);
            ExifInterface exif = new ExifInterface(getRealPathFromURI(getApplicationContext(), uriImage));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            System.out.println("ORIENTATION: " + orientation);

            if (orientation == 6)
                bmPhoto = rotateBitmap(bmPhoto, 90);
            else if (orientation == 8)
                bmPhoto = rotateBitmap(bmPhoto, 270);

            bmPhoto = cropSquare(bmPhoto);

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (bmPhoto != null) {
            bmNewPhoto = bmPhoto;
            ivCurrentProfilePhoto.setImageBitmap(bmNewPhoto);
        }
    }

    public String getBase64(Bitmap bmPhoto) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bmPhoto.compress(Bitmap.CompressFormat.JPEG, 100, os);
        return Base64.encodeToString(os.toByteArray(), Base64.DEFAULT);
    }

    public void sendNewPhoto(Bitmap bmPhoto) {

        sbUploading = QQData.showInfoInfinite(ProfilePhotoActivity.this, "Kaydediliyor...");

        String photo64 = getBase64(bmPhoto);
        System.out.println("photo64: " + photo64);

        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(30000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID))
                .add("photo64", photo64);

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sUploadProfilePhotoAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                sbUploading.dismiss();
                getResult_SendPhoto(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                sbUploading.dismiss();
                QQData.showError(ProfilePhotoActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_SendPhoto(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 911) {   // uploaded photo
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    QQData.MyInfo.sPPName = jsonRet.getString("pp");
                    QQData.m_actHome.fMyProfile.f_updateProfilePhoto();
                    QQData.m_actHome.fMyProfile.f_updateItemPhotos();
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(ProfilePhotoActivity.this, "İnternet bağlantısı kurulamadı");
            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public void changePhoto(View v) {
        Intent Picker = new Intent(Intent.ACTION_PICK);
        Picker.setType("image/*");
        Bundle translate = ActivityOptions.makeCustomAnimation(ProfilePhotoActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
        startActivityForResult(Picker, 100, translate);
    }

    public void savePhoto(View v) {
        if (bmNewPhoto != null) {
            sendNewPhoto(bmNewPhoto);
        } else {
            finish();
        }
    }

    public void removePhoto(View v) {
        if (QQData.MyInfo.sPPName == null) {
            finish();
            return;
        }

        QQData.showConfirm(ProfilePhotoActivity.this, "Fotoğrafı kaldırmak istiyor musunuz?", new ActionClickListener() {
            @Override
            public void onActionClicked(Snackbar snackbar) {
                sendRemovePhoto();
            }
        });
    }

    public void sendRemovePhoto() {
        AndroidHttpClient httpClient = new AndroidHttpClient(QQData.m_sServer);
        httpClient.setConnectionTimeout(10000);
        httpClient.setMaxRetries(3);
        ParameterMap params = httpClient.newParams()
                .add("uid", String.valueOf(QQData.m_iUID))
                .add("sid", String.valueOf(QQData.m_iSID));

        PostRequest pr = new PostRequest(httpClient, params, QQData.m_sRemoveProfilePhotoAdr, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                getResult_RemovePhoto(httpResponse);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                QQData.showError(ProfilePhotoActivity.this, "İnternet bağlantısı kurulamadı");
            }
        });

        QQData.pushPostRequest(pr);
    }

    public void getResult_RemovePhoto(HttpResponse httpResponse) {

        String sRet = httpResponse.getBodyAsString();
        if (QQData.isJSON(sRet)) {

            try {
                JSONObject jsonRet = new JSONObject(sRet);
                String retval = jsonRet.getString("ret");
                int iretval = Integer.parseInt(retval);

                if (iretval == 921) {   // removed
                    QQData.m_iSID = Integer.parseInt(jsonRet.getString("sid"));
                    ivCurrentProfilePhoto.setImageBitmap(null);
                    bmNewPhoto = null;
                    QQData.MyInfo.sPPName = null;

                    QQData.m_actHome.fMyProfile.f_updateProfilePhoto();
                    QQData.m_actHome.fMyProfile.f_updateItemPhotos();
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            int ret = Integer.parseInt(sRet);
            if (ret == -1) {   // db / param error
                QQData.showError(ProfilePhotoActivity.this, "İnternet bağlantısı kurulamadı");

            } else if (ret == -3) { //sid error
                QQData.m_actHome.doLogout();
            }
        }
    }

    public Bitmap rotateBitmap(Bitmap source, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap ret = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        return ret;
    }

    public Bitmap cropSquare(Bitmap srcBmp) {
        Bitmap dstBmp = srcBmp;

        if (srcBmp.getWidth() >= srcBmp.getHeight()) {

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth() / 2 - srcBmp.getHeight() / 2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );

        } else {

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight() / 2 - srcBmp.getWidth() / 2,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );
        }

        return dstBmp;
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public Bitmap decodeSampledBitmap(Uri image, int reqWidth, int reqHeight) throws IOException {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(image), null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap ret = BitmapFactory.decodeStream(getContentResolver().openInputStream(image), null, options);

        return ret;
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean dragClose(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            fDragY = event.getY();
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (event.getY() - fDragY >= 250.0) {
                finish();
            }
        }

        return true;
    }

}
