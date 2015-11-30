package com.gmail.radioserver2.radiko;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;

import com.gmail.radioserver2.data.Setting;
import com.gmail.radioserver2.utils.AndroidUtil;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by luhonghai on 2/20/15.
 */
public abstract class TokenFetcher {

    private static final long MAX_TOKEN_AGE = 10 * 60 * 1000;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public static interface OnTokenListener {

        public void onTokenFound(String token, String rawAreaId);

        public void onError(String message, Throwable throwable);
    }

    public static class RadikoToken {

        private String token;
        private String rawAreaId;
        private long timestamp;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getRawAreaId() {
            return rawAreaId;
        }

        public void setRawAreaId(String rawAreaId) {
            this.rawAreaId = rawAreaId;
        }
    }

    private final Context context;

    private final OnTokenListener onTokenListener;

    protected final FileHelper fileHelper;

    private double longitude = -1;

    private double latitude = -1;

    public TokenFetcher(Context context, OnTokenListener onTokenListener) {
        this.onTokenListener = onTokenListener;
        this.context = context;
        fileHelper = new FileHelper(context);
    }

    protected Context getContext() {
        return context;
    }

    public void fetch() {
        SimpleAppLog.info("Start fetch token");
        File savedToken = fileHelper.getTokenFile(getPrefixName());
        String token = "";
        if (savedToken.exists()) {
            SimpleAppLog.info("Found token cache");
            Gson gson = new Gson();
            try {
                RadikoToken radikoToken = gson.fromJson(FileUtils.readFileToString(savedToken, "UTF-8"), RadikoToken.class);
                if ((System.currentTimeMillis() - radikoToken.getTimestamp()) <= MAX_TOKEN_AGE) {
                    token = radikoToken.token;
                    SimpleAppLog.info("Cached token: " + token);
                    onTokenFound(token, radikoToken.rawAreaId);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SimpleAppLog.info("No cached token found");
        AsyncTask<Void, Void, Void> getTokenTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                SimpleAppLog.info("Request remote token");
                fetchRemote();
                return null;
            }
        };
        getTokenTask.execute();
    }

    public void clearTokenCache() {
        File savedToken = fileHelper.getTokenFile(getPrefixName());
        if (savedToken.exists()) {
            try {
                FileUtils.forceDelete(savedToken);
            } catch (Exception e) {
                SimpleAppLog.error("Could not clear token cache", e);
            }
        }
    }

    protected abstract void fetchRemote();

    protected abstract String getPrefixName();

    protected void saveToken(String token, String rawAreaId) throws IOException {
        Gson gson = new Gson();
        RadikoToken radikoToken = new RadikoToken();
        radikoToken.setRawAreaId(rawAreaId);
        radikoToken.setToken(token);
        radikoToken.setTimestamp(System.currentTimeMillis());
        FileUtils.writeStringToFile(fileHelper.getTokenFile(getPrefixName()), gson.toJson(radikoToken));
    }

    protected void onTokenFound(String token, String rawAreaId) {
        if (onTokenListener != null)
            onTokenListener.onTokenFound(token, rawAreaId);
    }

    protected void onError(String message, Throwable throwable) {
        if (onTokenListener != null)
            onTokenListener.onError(message, throwable);
    }

    public static TokenFetcher getTokenFetcher(Context context, String cookies, OnTokenListener onTokenListener) {
        Location location = AndroidUtil.getLastBestLocation(context);
        return getTokenFetcher(context, cookies, onTokenListener, location);
    }

    public static TokenFetcher getTokenFetcher(Context context, String cookies, OnTokenListener onTokenListener,
                                               Location location) {
        if (location == null) {
            location = AndroidUtil.getLastBestLocation(context);
        }
        if (location != null) {
            return getTokenFetcher(context, cookies, onTokenListener, location.getLatitude(), location.getLongitude());
        } else {
            return getTokenFetcher(context, cookies, onTokenListener, -1, -1);
        }
    }

    public static TokenFetcher getTokenFetcher(Context context, String cookies, OnTokenListener onTokenListener, double latitude, double longitude) {
        Setting setting = new Setting(context);
        setting.load();
        TokenFetcher tokenFetcher;
        if (setting.getTokenType() == Setting.TOKEN_TYPE_CLIENT) {
            SimpleAppLog.info("Use client token");
            tokenFetcher = new ClientTokenFetcher(context, cookies, onTokenListener);
        } else {
            SimpleAppLog.info("Use server token");
            tokenFetcher = new ServerTokenFetcher(context, onTokenListener);
        }
        tokenFetcher.setLatitude(latitude);
        tokenFetcher.setLongitude(longitude);
        return tokenFetcher;
    }
}
