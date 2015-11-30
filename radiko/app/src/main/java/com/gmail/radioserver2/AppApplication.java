package com.gmail.radioserver2;

/*Copyright*/

import android.app.Application;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.gmail.radioserver2.utils.AppDelegate;
import com.gmail.radioserver2.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
        AppDelegate.getInstance().setPremium(sharedPreferences.getBoolean(Constants.KEY_PREMIUM, false));
        String cookies = sharedPreferences.getString("Cookie", "");
        if (cookies.length() != 0) {
            AppDelegate.getInstance().setCookie(cookies);
        }
    }

}
