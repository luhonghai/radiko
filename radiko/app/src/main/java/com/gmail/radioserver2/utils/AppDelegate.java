package com.gmail.radioserver2.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;

public class AppDelegate {
    private static AppDelegate instance;

    private boolean isPremium = false;
    private String cookie;

    private AppDelegate() {

    }

    public static AppDelegate getInstance() {
        if (instance == null) {
            instance = new AppDelegate();
        }
        return instance;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public void setPremium(boolean isPremium) {
        this.isPremium = isPremium;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public static boolean hasPermission(Context context, String permission) {
        if (context == null) {
            return true;
        }
        return ActivityCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context == null || permissions == null || permissions.length == 0) {
            return true;
        }
        boolean grantedAll = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PermissionChecker.PERMISSION_GRANTED) {
                grantedAll = false;
                break;
            }
        }
        return grantedAll;
    }

    public static boolean isGrantedAll(@NonNull int[] grantResults) {
        boolean grantedAll = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                grantedAll = false;
                break;
            }
        }
        return grantedAll;
    }

    public static void requestPermission(Activity activity, int requestCode, String... permissions) {
        if (activity != null) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    private boolean isShowDialog = true;

    public void setShowDialog(boolean showDialog) {
        isShowDialog = showDialog;
    }

    public boolean isShowDialog() {
        return isShowDialog;
    }

}
