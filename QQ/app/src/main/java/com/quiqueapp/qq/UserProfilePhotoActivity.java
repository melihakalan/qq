package com.quiqueapp.qq;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.faradaj.blurbehind.BlurBehind;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.core.imageaware.NonViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class UserProfilePhotoActivity extends Activity {

    private float fDragY = 0;
    private ImageView ivUserPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_photo);

        QQData.m_lActivityList.add(UserProfilePhotoActivity.this);

        ivUserPhoto = (ImageView) findViewById(R.id.ivUserPhoto);

        ivUserPhoto.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return dragClose(v, event);
            }
        });

        BlurBehind.getInstance().withAlpha(255).setBackground(this);

        getPP();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_fade_in, R.anim.anim_drag_down);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(UserProfilePhotoActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(UserProfilePhotoActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(UserProfilePhotoActivity.this);
        }
    }

    public void getPP() {
        Bundle b = getIntent().getExtras();
        String pp = b.getString("pp");

        ImageLoader.getInstance().displayImage(QQData.m_sPPAdr + pp, new NonViewAware(new ImageSize(320, 320), ViewScaleType.CROP), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                ivUserPhoto.setImageBitmap(loadedImage);
            }
        });
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
