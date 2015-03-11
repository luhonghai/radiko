package com.gmail.radioserver2.service;

import android.content.Context;
import android.content.Intent;

import com.dotohsoft.api.TokenRequester;
import com.dotohsoft.radio.Constant;
import com.dotohsoft.radio.api.APIRequester;
import com.dotohsoft.radio.data.RadioArea;
import com.dotohsoft.radio.data.RadioChannel;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Setting;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by luhonghai on 3/11/15.
 */
public class DataPrepareService {

    private final Context context;
    private FileHelper fileHelper;
    private Setting setting;

    public DataPrepareService(Context context) {
        this.context = context;
        fileHelper = new FileHelper(context);
        setting = new Setting(context);
    }

    public void execute() {
        setting.load();
        TokenFetcher tokenFetcher = TokenFetcher.getTokenFetcher(context, new TokenFetcher.OnTokenListener() {
            @Override
            public void onTokenFound(String token, String rawAreaId) {
                requestChannels(rawAreaId);
            }

            @Override
            public void onError(String message, Throwable throwable) {
                SimpleAppLog.error(message, throwable);
            }
        });
        tokenFetcher.fetch();
    }

    private void requestChannels(String rawAreaId) {
        APIRequester apiRequester = new APIRequester(fileHelper.getApiCachedFolder());
        String defaultChannel = "";
        InputStream is = null;
        try {
            is =context.getResources().openRawResource(R.raw.default_channels);
            defaultChannel = IOUtils.toString(is);
        } catch (IOException e) {
            SimpleAppLog.error("Could not fetch default channels from resource",e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {

                }
            }
        }
        try {
            RadioChannel radioChannel = apiRequester.getChannels(rawAreaId, setting.isRegion(), defaultChannel);
            if (radioChannel != null) {
                ChannelDBAdapter dbAdapter = new ChannelDBAdapter(context);
                try {
                    dbAdapter.open();
                    List<RadioChannel.Channel> channelList = radioChannel.getChannels();
                    if (channelList != null && channelList.size() > 0) {
                        for (RadioChannel.Channel channel : channelList) {
                            Channel dbChannel = new Channel();
                            dbChannel.setName(channel.getName());
                            dbChannel.setType(channel.getService());
                            dbChannel.setUrl(channel.getStreamURL());
                            dbChannel.setKey(channel.getServiceChannelId());
                            dbAdapter.insert(dbChannel);
                        }
                    }
                    Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                    intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_RELOAD_LIST);
                    context.sendBroadcast(intent);
                } catch (Exception e) {
                    SimpleAppLog.error("Could not insert channel", e);
                } finally {
                    dbAdapter.close();
                }
            }
        } catch (IOException e) {
            SimpleAppLog.error("Could not get channel", e);
        }
    }
}
