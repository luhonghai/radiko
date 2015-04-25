package com.gmail.radioserver2.utils;

import android.content.Context;
import android.content.res.Configuration;

import com.gmail.radioserver2.R;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by luhonghai on 26/02/2015.
 */
public class AndroidUtil {

    public static void updateLanguage(final Context context) {
        updateLanguage(context, context.getResources().getString(R.string.default_language));
    }

    public static void updateLanguage(final Context context, String locate) {
        Configuration c = new Configuration(context.getResources().getConfiguration());
        if (locate.equalsIgnoreCase("ja")) {
            c.locale = Locale.JAPANESE;
        } else if (locate.equalsIgnoreCase("en")) {
            c.locale = Locale.ENGLISH;
        } else {
            c.locale = new Locale(locate);
        }
        context.getResources().updateConfiguration(c, context.getResources().getDisplayMetrics());
    }

    // Do not call this function from the main thread. Otherwise,
    // an IllegalStateException will be thrown.
    public static String getAdsId(Context mContext) {
        AdvertisingIdClient.Info adInfo = null;
        try {
            adInfo = AdvertisingIdClient.getAdvertisingIdInfo(mContext);
        } catch (Exception e) {
            // Google Play services is not available entirely.
            SimpleAppLog.error("Could not fetch Ads ID",e);
        }
        return adInfo == null ? "" : adInfo.getId();
    }
}
