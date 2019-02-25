package com.quiqueapp.qq;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

/**
 * Created by user on 11.1.2016.
 */
public class InteractiveScrollView extends ScrollView {
    OnBottomReachedListener mListener;

    public InteractiveScrollView(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
    }

    public InteractiveScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InteractiveScrollView(Context context) {
        super(context);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        if (mListener != null) {
            View view = getChildAt(getChildCount() - 1);
            int diff = (view.getBottom() - (getHeight() + getScrollY()));

            if (diff == 0) {
                mListener.onBottomReached();
            }
        }

        super.onScrollChanged(l, t, oldl, oldt);
    }


    // Getters & Setters

    public OnBottomReachedListener getOnBottomReachedListener() {
        return mListener;
    }

    public void setOnBottomReachedListener(
            OnBottomReachedListener onBottomReachedListener) {
        mListener = onBottomReachedListener;
    }


    /**
     * Event listener.
     */
    public interface OnBottomReachedListener {
        void onBottomReached();
    }

}