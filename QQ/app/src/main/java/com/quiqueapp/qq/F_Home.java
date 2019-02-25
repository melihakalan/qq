package com.quiqueapp.qq;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.ogaclejapan.smarttablayout.utils.v13.FragmentPagerItemAdapter;
import com.ogaclejapan.smarttablayout.utils.v13.FragmentPagerItems;

/**
 * Created by user on 17.11.2015.
 */

public class F_Home extends Fragment {

    public F_Home_Following fHomeFollowing = null;
    public F_Home_All fHomeAll = null;
    public F_Home_Trend fHomeTrend = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.f_home, container, false);

        FragmentPagerItemAdapter fpia = new FragmentPagerItemAdapter(
                getFragmentManager(), FragmentPagerItems.with(v.getContext())
                .add("TAKİP", F_Home_Following.class)
                .add("GÜNDEM", F_Home_All.class)
                .add("POPÜLER", F_Home_Trend.class)
                .create());

        ViewPager vpHome = (ViewPager) v.findViewById(R.id.vpHome);
        vpHome.setAdapter(fpia);
        vpHome.setOffscreenPageLimit(3);

        SmartTabLayout stlHome = (SmartTabLayout) v.findViewById(R.id.stlHome);
        stlHome.setViewPager(vpHome);

        return v;
    }

    public void f_nearQuestionsClick(View v) {
        Intent it = new Intent(QQData.m_actHome, LocationQuestionsActivity.class);
        Bundle translate = ActivityOptions.makeCustomAnimation(QQData.m_actHome, R.anim.act_slide_in_left, R.anim.act_slide_out_left).toBundle();
        startActivity(it, translate);
    }

}