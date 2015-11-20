package com.gmail.radioserver2.utils;

/*Copyright*/

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;

public class AppDelegate {
    private static AppDelegate instance;

    private String userName, password;
    private boolean isPremium = false;

    private AppDelegate() {

    }

    public static AppDelegate getInstance() {
        if (instance == null) {
            instance = new AppDelegate();
        }
        return instance;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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


}
