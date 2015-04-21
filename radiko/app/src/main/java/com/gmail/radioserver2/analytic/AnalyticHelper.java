package com.gmail.radioserver2.analytic;

import android.content.Context;

import com.gmail.radioserver2.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by luhonghai on 4/17/15.
 */
public class AnalyticHelper {

    public static final String CATEGORY_AREA_ID = "Area ID";

    public static final String ACTION_ERROR = "ERROR";

    public static final String ACTION_NULL = "NULL";

    public static synchronized Tracker getTracker(Context context) {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
        Tracker t = analytics.newTracker(R.xml.global_tracker);
        t.enableAdvertisingIdCollection(true);
        t.enableAutoActivityTracking(true);
        t.enableExceptionReporting(true);
        return t;
    }

}
