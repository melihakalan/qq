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
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SetOptionImageActivity extends Activity {

    private float fDragY = 0;
    private ImageView ivOptionImageFull;
    private Bitmap bmNewPhoto = null;
    private int iOption;
    private File fTakenImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_option_image);

        QQData.m_lActivityList.add(SetOptionImageActivity.this);

        ivOptionImageFull = (ImageView) findViewById(R.id.ivOptionImageFull);

        ivOptionImageFull.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return dragClose(v, event);
            }
        });

        getInfo();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_fade_in, R.anim.anim_drag_down);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(SetOptionImageActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(SetOptionImageActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(SetOptionImageActivity.this);
        }
    }

    public void getInfo() {
        Bundle b = getIntent().getExtras();
        iOption = b.getInt("option");

        if (QQData.m_actHome.fPostQuestion.bmPostImages[iOption] != null)
            ivOptionImageFull.setImageBitmap(QQData.m_actHome.fPostQuestion.bmPostImages[iOption]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case 100:    // SELECT_PHOTO
                if (resultCode == RESULT_OK) {
                    getNewPhoto(imageReturnedIntent.getData(), false);
                }
                break;

            case 1:    // REQUEST_IMAGE_CAPTURE
                if (resultCode == RESULT_OK) {
                    getNewPhoto(Uri.fromFile(fTakenImage), true);
                }
                break;
        }
    }

    public void getNewPhoto(Uri uriImage, boolean taking) {
        Bitmap bmPhoto = null;

        try {
            bmPhoto = decodeSampledBitmap(uriImage, 320, 320);
            ExifInterface exif = null;
            if (!taking)
                exif = new ExifInterface(getRealPathFromURI(getApplicationContext(), uriImage));
            else
                exif = new ExifInterface(fTakenImage.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            System.out.println("ORIENTATION: " + orientation);

            if (orientation == 6)
                bmPhoto = rotateBitmap(bmPhoto, 90);
            else if (orientation == 8)
                bmPhoto = rotateBitmap(bmPhoto, 270);

            //bmPhoto = cropSquare(bmPhoto);

            if (taking) {
                fTakenImage.delete();
                fTakenImage = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (bmPhoto != null) {
            bmNewPhoto = bmPhoto;
            ivOptionImageFull.setImageBitmap(bmNewPhoto);
        }
    }

    public void changeOptionImage(View v) {
        Intent Picker = new Intent(Intent.ACTION_PICK);
        Picker.setType("image/*");
        Bundle translate = ActivityOptions.makeCustomAnimation(SetOptionImageActivity.this, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
        startActivityForResult(Picker, 100, translate);
    }

    public void saveOptionImage(View v) {
        if (bmNewPhoto != null) {
            Bitmap bmThumb = getBitmapThumb(bmNewPhoto);
            QQData.m_actHome.fPostQuestion.updateImage(iOption, bmNewPhoto, bmThumb);
            finish();
        } else {
            finish();
        }
    }

    public void removeOptionImage(View v) {
        QQData.m_actHome.fPostQuestion.updateImage(iOption, null, null);
        finish();
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

    public Bitmap getBitmapThumb(Bitmap bm) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 64, 64);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap ret = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size(), options);

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

    public void takeOptionImage(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, 1);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        fTakenImage = image;
        return image;
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
