package com.gmail.radioserver2.radiko;

import android.content.Context;

import com.dotohsoft.api.TokenRequester;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.utils.SimpleAppLog;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import jp.radiko.k.k;

/**
 * Created by luhonghai on 24/02/2015.
 */
public class ClientTokenFetcher extends TokenFetcher {

    public ClientTokenFetcher(Context context, OnTokenListener onTokenListener) {
        super(context, onTokenListener);
    }

    @Override
    protected void fetchRemote() {
        File keyBin = fileHelper.getKeyBinFile();
        if (!keyBin.exists()) {
            InputStream is = null;
            try {
                //is = getContext().getResources().openRawResource(R.raw.key_bin);
                is = getContext().getResources().openRawResource(R.raw.asmartphone4);
                FileUtils.copyInputStreamToFile(is, keyBin);
            } catch (Exception ex) {
                SimpleAppLog.error("Could not get key bin", ex);
            } finally {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException e) {}
            }
        }

        TokenRequester requester = new TokenRequester(new TokenRequester.TokenRequesterListener() {
            @Override
            public void onMessage(String message) {
                SimpleAppLog.info(message);
            }

            @Override
            public void onError(String message, Throwable e) {
                SimpleAppLog.error(message, e);
            }
        }, keyBin, new TokenRequester.TokenExchange() {
            @Override
            public byte[] exchange(byte[] input) {
                return k.getKeyNative(getContext(), input);
            }
        });
        String token = "";
        String output = "";
        try {
            TokenRequester.TokenData tokenData = requester.requestToken(getLatitude(), getLongitude());
            if (tokenData != null) {
                saveToken(tokenData.getToken(), tokenData.getOutput());
                token = tokenData.getToken();
                output = tokenData.getOutput();
            } else {

            }
        } catch (Exception e) {
            onError("Could not fetch token",e);
        }
        onTokenFound(token, output);
    }

    @Override
    protected String getPrefixName() {
        return "client";
    }
}
