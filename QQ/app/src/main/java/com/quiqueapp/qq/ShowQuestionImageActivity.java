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

public class ShowQuestionImageActivity extends Activity {

    private float fDragY = 0;
    private ImageView ivShowQuestionImage;
    private CircularProgressBar cpbQuestionImageLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_question_image);

        QQData.m_lActivityList.add(ShowQuestionImageActivity.this);

        ivShowQuestionImage = (ImageView) findViewById(R.id.ivShowQuestionImage);
        cpbQuestionImageLoading = (CircularProgressBar) findViewById(R.id.cpbQuestionImageLoading);

        ivShowQuestionImage.setOnTouchListener(new View.OnTouchListener() {
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

        QQData.m_lVisibleActivityList.add(ShowQuestionImageActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        QQData.m_lVisibleActivityList.remove(ShowQuestionImageActivity.this);
        if (isFinishing()) {
            QQData.m_lActivityList.remove(ShowQuestionImageActivity.this);
        }
    }

    public void getImage() {
        Bundle b = getIntent().getExtras();
        String image = b.getString("image");

        ImageLoader.getInstance().displayImage(QQData.m_sQuestionImageAdr + image, new NonViewAware(new ImageSize(320, 320), ViewScaleType.CROP), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                cpbQuestionImageLoading.setVisibility(View.GONE);
                ivShowQuestionImage.setVisibility(View.VISIBLE);
                ivShowQuestionImage.setImageBitmap(loadedImage);
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
