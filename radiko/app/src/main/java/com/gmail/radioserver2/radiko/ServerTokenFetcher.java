package com.gmail.radioserver2.radiko;

import android.content.Context;
import android.os.AsyncTask;

import com.dotohsoft.radio.data.RadioArea;
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
        final File tmpFile = fileHelper.getTmpTokenFile();
        try {
            if (tmpFile.exists())
                FileUtils.forceDelete(tmpFile);
            FileUtils.copyURLToFile(new URL("http://stest.dotohsoft.com/~duc/rad/gettoken/getkey.php"), tmpFile);
            if (tmpFile.exists()) {
                String token = FileUtils.readFileToString(tmpFile, "UTF-8");
                saveToken(token, RadioArea.AREA_ID_TOKYO);
                onTokenFound(token, RadioArea.AREA_ID_TOKYO);
            } else {
                onTokenFound("", "");
            }

        } catch (Exception e) {
            onError("Could not connect to server", e);
        }
    }

    @Override
    protected String getPrefixName() {
        return "server";
    }
}
