package com.gmail.radioserver2.radiko;

import android.content.Context;

import com.gmail.radioserver2.radiko.token.TokenRequester;

import java.io.IOException;

/**
 * Created by luhonghai on 24/02/2015.
 */
public class ClientTokenFetcher extends TokenFetcher {

    public ClientTokenFetcher(Context context, OnTokenListener onTokenListener) {
        super(context, onTokenListener);
    }

    @Override
    protected void fetchRemote() {
        TokenRequester requester = new TokenRequester(getContext());
        try {
            String token = requester.requestToken();
            if (token.length() > 0) saveToken(token);
            onTokenFound(token);
        } catch (Exception e) {
            onError("Could not fetch token",e);
        }

    }
}
