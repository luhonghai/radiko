package com.gmail.radioserver2.radiko;

import android.content.Context;
import android.os.AsyncTask;

import com.gmail.radioserver2.utils.FileHelper;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by luhonghai on 2/20/15.
 */
public abstract class TokenFetcher {

    private static final long MAX_TOKEN_AGE = 10 * 60 * 1000;

    public static interface OnTokenListener {

        public void onTokenFound(String token);

        public void onError(String message, Throwable throwable);
    }

    public static class RadikoToken {

        private String token;
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
    }

    private final Context context;

    private final OnTokenListener onTokenListener;

    protected final FileHelper fileHelper;

    public TokenFetcher(Context context, OnTokenListener onTokenListener) {
        this.onTokenListener = onTokenListener;
        this.context = context;
        fileHelper = new FileHelper(context);
    }

    protected Context getContext() {
        return context;
    }

    public void fetch() {
        File savedToken = fileHelper.getTokenFile(getPrefixName());
        String token = "";
        if (savedToken.exists()) {
            Gson gson = new Gson();
            try {
                RadikoToken radikoToken = gson.fromJson(FileUtils.readFileToString(savedToken, "UTF-8"), RadikoToken.class);
                if ((System.currentTimeMillis() - radikoToken.getTimestamp()) <= MAX_TOKEN_AGE) {
                    token = radikoToken.token;
                    onTokenFound(token);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (token.length() == 0) {
            AsyncTask<Void, Void, Void> getTokenTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    fetchRemote();
                    return null;
                }
            };
            getTokenTask.execute();
        }
    }

    protected abstract void fetchRemote();

    protected abstract String getPrefixName();

    protected void saveToken(String token) throws IOException {
        Gson gson = new Gson();
        RadikoToken radikoToken = new RadikoToken();
        radikoToken.setToken(token);
        radikoToken.setTimestamp(System.currentTimeMillis());
        FileUtils.writeStringToFile(fileHelper.getTokenFile(getPrefixName()), gson.toJson(radikoToken));
    }

    protected void onTokenFound(String token) {
        if (onTokenListener != null)
            onTokenListener.onTokenFound(token);
    }

    protected void onError(String message, Throwable throwable) {
        if (onTokenListener != null)
            onTokenListener.onError(message, throwable);
    }
}
