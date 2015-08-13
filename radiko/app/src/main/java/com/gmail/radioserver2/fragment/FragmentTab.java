package com.gmail.radioserver2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.gmail.radioserver2.analytic.AnalyticHelper;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by luhonghai on 2/17/15.
 * <p/>
 * Base fragment tab
 */
public class FragmentTab extends SherlockFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Tracker t = AnalyticHelper.getTracker(getActivity());
        t.setScreenName(this.getClass().getName());
        t.send(new HitBuilders.ScreenViewBuilder().build());
        return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //
    }
}
