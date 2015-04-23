package com.gmail.radioserver2.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.crashlytics.android.Crashlytics;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.analytic.AnalyticHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.location.LocationRequest;

import io.fabric.sdk.android.Fabric;

/**
 * Created by luhonghai on 2/16/15.
 */

public abstract class BaseFragmentActivity extends SherlockFragmentActivity implements LocationListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tracker t = AnalyticHelper.getTracker(this);
        t.setScreenName(this.getClass().getName());
        t.send(new HitBuilders.ScreenViewBuilder().build());
       // AndroidUtil.updateLanguage(this);
        Fabric.with(this, new Crashlytics());

        Thread.UncaughtExceptionHandler myHandler = new ExceptionReporter(
                t,
                Thread.getDefaultUncaughtExceptionHandler(),
                this);
        Thread.setDefaultUncaughtExceptionHandler(myHandler);

        locationCheck();
        updateChannels();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public abstract void updateChannels();

    @Override
    public void onLocationChanged(Location location) {
        SimpleAppLog.info("onLocationChanged");
        updateChannels();
    }


    private void updateSetting() {
        
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //updateChannels();
    }

    @Override
    public void onProviderEnabled(String provider) {
        SimpleAppLog.info("onProviderEnabled");
        updateChannels();
    }

    @Override
    public void onProviderDisabled(String provider) {
        //updateChannels();
    }

    protected void locationCheck() {
        LocationManager lm = null;
        boolean gps_enabled = false,network_enabled = false;
        if(lm==null)
            lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try{
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 60 * 1000, 1000, this);
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){}
        try{
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10 * 60 * 1000, 1000, this);
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){}

        if(!gps_enabled && !network_enabled){
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.gps_warning_title));
            dialog.setMessage(getString(R.string.gps_warning_message));
            dialog.setPositiveButton(getString(R.string.gps_warning_positive_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    BaseFragmentActivity.this.startActivity(myIntent);
                }
            });
            dialog.setNegativeButton(getString(R.string.gps_warning_negative_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                }
            });
            dialog.show();
        }
    }
}
