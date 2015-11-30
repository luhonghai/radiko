package com.gmail.radioserver2.activity;

import android.Manifest.permission;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import static com.gmail.radioserver2.utils.AppDelegate.hasPermission;
import static com.gmail.radioserver2.utils.AppDelegate.hasPermissions;
import static com.gmail.radioserver2.utils.AppDelegate.isGrantedAll;
import static com.gmail.radioserver2.utils.AppDelegate.requestPermission;

/**
 * Created by luhonghai on 26/02/2015.
 */
public class SplashActivity extends BaseActivity {

    private static final int REQUEST_INTERNET_PERMISSION = 1;
    private static final int REQUEST_WRITE_FILE_PERMISSION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        FacebookSdk.sdkInitialize(getApplicationContext());
        if (!hasPermissions(this, permission.ACCESS_NETWORK_STATE, permission.ACCESS_WIFI_STATE, permission.INTERNET)) {
            requestPermission(this, REQUEST_INTERNET_PERMISSION,
                    permission.ACCESS_NETWORK_STATE, permission.ACCESS_WIFI_STATE, permission.INTERNET);
        } else if (!hasPermissions(this, permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE)) {
            requestPermission(this, REQUEST_WRITE_FILE_PERMISSION, permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE);
        } else {
            gotoMainActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_INTERNET_PERMISSION) {
            if (isGrantedAll(grantResults)) {
                if (hasPermission(this, permission.WRITE_EXTERNAL_STORAGE)) {
                    gotoMainActivity();
                } else {
                    requestPermission(this, REQUEST_WRITE_FILE_PERMISSION, permission.WRITE_EXTERNAL_STORAGE);
                }
            } else {
                Toast.makeText(this, "App may work incorrectly, please accept all requested permission", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == REQUEST_WRITE_FILE_PERMISSION) {
            if (isGrantedAll(grantResults)) {
                gotoMainActivity();
            } else {
                Toast.makeText(this, "App may work incorrectly, please accept all requested permission", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            gotoMainActivity();
        }
    }

    private void gotoMainActivity() {
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
