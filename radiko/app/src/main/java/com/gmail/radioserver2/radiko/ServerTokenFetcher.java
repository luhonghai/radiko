package com.gmail.radioserver2.radiko;

import android.content.Context;
import android.os.AsyncTask;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.utils.FileHelper;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by luhonghai on 2/21/15.
 */
public class ServerTokenFetcher extends TokenFetcher {

    public ServerTokenFetcher(Context context, OnTokenListener onTokenListener) {
        super(context, onTokenListener);
    }

    @Override
    public void fetchRemote() {
        AsyncTask<Void, Void, Void> getTokenTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final File tmpFile = fileHelper.getTmpTokenFile();
                try {
                    if (tmpFile.exists())
                        FileUtils.forceDelete(tmpFile);
                    FileUtils.copyURLToFile(new URL(getContext().getString(R.string.radiko_server_token_url)), tmpFile);
                    if (tmpFile.exists()) {
                        String token = FileUtils.readFileToString(tmpFile, "UTF-8");
                        saveToken(token);
                        onTokenFound(token);
                    } else {
                        onTokenFound("");
                    }

                } catch (IOException e) {
                    onError("Could not connect to server", e);
                }
                return null;
            }
        };
        getTokenTask.execute();
    }
}
