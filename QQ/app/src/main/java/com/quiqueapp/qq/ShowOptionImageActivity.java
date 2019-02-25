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

import fr.castorflex.android.circularprogressbar.CircularProgressBar;

public class ShowOptionImageActivity extends Activity {

    private float fDragY = 0;
    private ImageView ivShowOptionImage;
    private CircularProgressBar cpbOptionImageLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_option_image);

        QQData.m_lActivityList.add(ShowOptionImageActivity.this);

        ivShowOptionImage = (ImageView) findViewById(R.id.ivShowOptionImage);
        cpbOptionImageLoading = (CircularProgressBar) findViewById(R.id.cpbOptionImageLoading);

        ivShowOptionImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return dragClose(v, event);
            }
        });

        BlurBehind.getInstance().withAlpha(255).setBackground(this);

        getImage();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.act_fade_in, R.anim.anim_drag_down);
    }

    @Override
    public void onResume() {
        super.onResume();

        QQData.m_lVisibleActivityList.add(ShowOptionImageActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(ShowOptionImageActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(ShowOptionImageActivity.this);
        }
    }

    public void getImage() {
        Bundle b = getIntent().getExtras();
        String image = b.getString("image");

        ImageLoader.getInstance().displayImage(QQData.m_sOptionImageAdr + image, new NonViewAware(new ImageSize(320, 320), ViewScaleType.CROP), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                cpbOptionImageLoading.setVisibility(View.GONE);
                ivShowOptionImage.setVisibility(View.VISIBLE);
                ivShowOptionImage.setImageBitmap(loadedImage);
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
