package com.gmail.radioserver2.activity;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.crashlytics.android.Crashlytics;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.analytic.AnalyticHelper;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.utils.AndroidUtil;
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

    private static final long UPDATE_DATA_TIMEOUT = 2000;

    private static final long UPDATE_DATA_MAX_TIMEOUT = 7000;

    private AlertDialog alertDialog;

    private Location currentLocation;

    private Runnable dataPrepareRunnable = new Runnable() {
        @Override
        public void run() {
            updateData();
        }
    };

    private Handler handler = new Handler();

    private void updateData() {
        SimpleAppLog.info("Start update channels");
        TokenFetcher.getTokenFetcher(this, null, null).clearTokenCache();
        updateChannels(AndroidUtil.filterLocation(currentLocation));
        currentLocation = null;
    }

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
        buildAlert();
        SimpleAppLog.info("Start update data after 2s");
        handler.removeCallbacks(dataPrepareRunnable);
        handler.post(dataPrepareRunnable);
        //checkWifiPolicy();
    }

    private void buildAlert() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.gps_warning_title));
        dialog.setMessage(getString(R.string.gps_warning_message));
        dialog.setPositiveButton(getString(R.string.gps_warning_positive_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                BaseFragmentActivity.this.startActivity(myIntent);
            }
        });
        dialog.setNegativeButton(getString(R.string.gps_warning_negative_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {

            }
        });
        alertDialog = dialog.create();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestLocation();
        //locationCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRequestLocation();
        if (alertDialog != null && alertDialog.isShowing())
            alertDialog.dismiss();
    }

    public abstract void updateChannels(Location location);

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        SimpleAppLog.info("onLocationChanged " + ( (location == null) ? " null" : location.getProvider() ));
        handler.removeCallbacks(dataPrepareRunnable);
        handler.postDelayed(dataPrepareRunnable, UPDATE_DATA_TIMEOUT);
    }

    private void updateSetting() {
        
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        SimpleAppLog.info("onStatusChanged " + provider + " status: " + status);
        currentLocation = null;
        handler.removeCallbacks(dataPrepareRunnable);
        handler.postDelayed(dataPrepareRunnable, UPDATE_DATA_TIMEOUT);
    }

    @Override
    public void onProviderEnabled(String provider) {
        currentLocation = null;
        SimpleAppLog.info("onProviderEnabled " + provider);
        handler.removeCallbacks(dataPrepareRunnable);
        handler.postDelayed(dataPrepareRunnable, UPDATE_DATA_TIMEOUT);
    }

    @Override
    public void onProviderDisabled(String provider) {
        currentLocation = null;
        SimpleAppLog.info("onProviderDisabled " + provider);
        handler.removeCallbacks(dataPrepareRunnable);
        handler.postDelayed(dataPrepareRunnable, UPDATE_DATA_TIMEOUT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    protected void stopRequestLocation() {
        SimpleAppLog.info("Request location update");
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            lm.removeUpdates(this);
        } catch (Exception e) {
            SimpleAppLog.error("Could not stop request location",e);
        }
    }

    protected void requestLocation() {
        SimpleAppLog.info("Request location update");
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 60 * 1000, 1000, this);
        } catch (Exception e) {
            SimpleAppLog.error("Could not request GPS provider location",e);
        }
        try {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5 * 60 * 1000, 1000, this);
        } catch (Exception e) {
            SimpleAppLog.error("Could not request Network provider location",e);
        }
    }

    protected void locationCheck() {
        LocationManager lm = null;
        boolean gps_enabled = false,network_enabled = false;
        if(lm==null)
            lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try{
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){}
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 60 * 1000, 1000, this);
        } catch (Exception e) {

        }
        try{
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){}
        try {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5 * 60 * 1000, 1000, this);
        } catch (Exception e) {}

        if(!gps_enabled && !network_enabled){
            if (alertDialog != null) {
                if (alertDialog.isShowing())
                    alertDialog.dismiss();
                alertDialog.show();
            }
        }
    }

    private boolean checkWifiPolicy() {
        WifiManager wm = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if(!wm.isWifiEnabled())
        {
            return true;
        }
        ContentResolver cr = this.getContentResolver();
        int policyNever = android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER;
        try
        {
            android.provider.Settings.System.putInt(cr, Settings.System.WIFI_SLEEP_POLICY, policyNever);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            if(android.provider.Settings.System.getInt(cr, Settings.System.WIFI_SLEEP_POLICY) != policyNever)
            {
                new AlertDialog.Builder(this).setTitle(getString(R.string.wifi_policy_warning_title))
                        .setMessage(getString(R.string.wifi_policy_warning_message))
                        .setPositiveButton(getString(R.string.wifi_policy_warning_positive_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Settings.ACTION_WIFI_IP_SETTINGS));
                            }
                        })
                        .setNegativeButton(getString(R.string.wifi_policy_warning_negative_button), null)
                        .show();
                return false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
}
