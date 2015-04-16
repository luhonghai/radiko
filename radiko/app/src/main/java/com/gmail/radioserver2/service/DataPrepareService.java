package com.gmail.radioserver2.service;

import android.content.Context;
import android.content.Intent;

import com.dotohsoft.api.TokenRequester;
import com.dotohsoft.radio.Constant;
import com.dotohsoft.radio.api.APIRequester;
import com.dotohsoft.radio.data.RadioArea;
import com.dotohsoft.radio.data.RadioChannel;
import com.dotohsoft.radio.data.RadioProvider;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Setting;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.utils.StringUtil;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
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
                requestChannels(StringUtil.escapeJapanSpecialChar(rawAreaId));
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
        apiRequester.setRequesterListener(new APIRequester.RequesterListener() {
            @Override
            public void onMessage(String message) {
                SimpleAppLog.info(message);
            }

            @Override
            public void onError(String error, Throwable throwable) {
                SimpleAppLog.error(error, throwable);
            }
        });
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
            RadioChannel radioChannel;
            SimpleAppLog.info("Load default channel: " + defaultChannel);
            SimpleAppLog.info("Raw area ID: " + rawAreaId);
            if (setting.getTokenType() == Setting.TOKEN_TYPE_SERVER) {
                radioChannel = apiRequester.getChannels(RadioArea.AREA_ID_TOKYO, setting.isRegion(), defaultChannel);
                //radioChannel = apiRequester.getChannels("JP6,%#$ASCDAS", setting.isRegion(), defaultChannel);
            } else {
                radioChannel = apiRequester.getChannels(rawAreaId, setting.isRegion(), defaultChannel);
            }
            ChannelDBAdapter dbAdapter = new ChannelDBAdapter(context);
            try {
                dbAdapter.open();
                if (radioChannel != null) {
                    Collection<Channel> channels = dbAdapter.findByProvider(RadioProvider.RADIKO);
                    List<RadioChannel.Channel> channelList = radioChannel.getChannels();
                    SimpleAppLog.info("Current Radiko channels: " + (channels == null ? "0" : channels.size()));
                    SimpleAppLog.info("Found channels size: " + (channelList == null ? "0" : channelList.size()));
                    if (channelList != null && channelList.size() > 0) {
                        for (RadioChannel.Channel channel : channelList) {
                            Channel dbChannel = new Channel();
                            dbChannel.setName(channel.getName());
                            dbChannel.setType(channel.getService());
                            dbChannel.setUrl(channel.getStreamURL());
                            dbChannel.setKey(channel.getServiceChannelId());
                            SimpleAppLog.info("Start insert channel " + dbChannel.getName());
                            dbAdapter.insert(dbChannel);
                            if (channels != null && channels.contains(dbChannel)) {
                                SimpleAppLog.info("Remove same URL " + dbChannel.getUrl());
                                channels.remove(dbChannel);
                            }
                        }
                    }
                    if (channels != null && channels.size() > 0) {
                        for (Channel channel : channels) {
                            SimpleAppLog.info("Delete unmatched Radiko channel " + channel.getName());
                            dbAdapter.delete(channel);
                        }
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

        } catch (IOException e) {
            SimpleAppLog.error("Could not get channel", e);
        }
    }
}
