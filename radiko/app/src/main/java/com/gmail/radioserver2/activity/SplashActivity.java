package com.gmail.radioserver2.activity;

import android.content.Intent;
import android.os.Bundle;

/**
 * Created by luhonghai on 26/02/2015.
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        startActivity(intent);
    }
}
