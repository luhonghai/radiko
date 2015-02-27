package com.gmail.radioserver2.utils;

import android.content.Context;
import android.content.res.Configuration;

import com.gmail.radioserver2.R;

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
}
