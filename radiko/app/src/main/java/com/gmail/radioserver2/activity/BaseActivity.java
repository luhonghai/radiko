package com.gmail.radioserver2.activity;

import android.content.res.Configuration;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.gmail.radioserver2.utils.AndroidUtil;

import java.util.Locale;

/**
 * Created by luhonghai on 2/21/15.
 */
public class BaseActivity extends SherlockActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidUtil.updateLanguage(this);
    }
}
