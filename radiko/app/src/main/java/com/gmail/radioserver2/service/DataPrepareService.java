package com.gmail.radioserver2.service;

import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.dotohsoft.radio.api.APIRequester;
import com.dotohsoft.radio.data.RadioArea;
import com.dotohsoft.radio.data.RadioChannel;
import com.dotohsoft.radio.data.RadioLocation;
import com.dotohsoft.radio.data.RadioProvider;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.analytic.AnalyticHelper;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Setting;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.utils.AndroidUtil;
import com.gmail.radioserver2.utils.AppDelegate;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;

/**
 * Created by luhonghai on 3/11/15.
 */
public class DataPrepareService {

    public static class RadioLocationContainer {
        public List<RadioLocation> getLocations() {
            return locations;
        }

        public void setLocations(List<RadioLocation> locations) {
            this.locations = locations;
        }

        private List<RadioLocation> locations;
    }

    private final Context context;
    private FileHelper fileHelper;
    private Setting setting;

    private Location location;

    public DataPrepareService(Context context, Location location) {
        this.context = context;
        fileHelper = new FileHelper(context);
        if (location != null && location.getLatitude() != -1 && location.getLongitude() != -1) {
            this.location = location;
        }
        setting = new Setting(context);
        setting.load();
    }

    public void execute() {
        SimpleAppLog.info("Start data prepare service");
        // Get tracker.
        final Tracker t = AnalyticHelper.getTracker(context);
        AndroidUtil.updateTokenType(context, location);
        TokenFetcher tokenFetcher = TokenFetcher.getTokenFetcher(context, AppDelegate.getInstance().getCookie(),
                new TokenFetcher.OnTokenListener() {
                    @Override
                    public void onTokenFound(String token, String rawAreaId) {
                        SimpleAppLog.info("On token found. Token: " + token + ". Area ID: " + rawAreaId);
                        requestChannels(token, rawAreaId);
                        t.send(new HitBuilders.EventBuilder()
                                .setCategory(AnalyticHelper.CATEGORY_AREA_ID)
                                .setAction(rawAreaId)
                                .build());
                    }

                    @Override
                    public void onError(String message, Throwable throwable) {
                        SimpleAppLog.info("On token error. Message: " + message);
                        SimpleAppLog.error(message, throwable);
                        //requestChannels("");
                        t.send(new HitBuilders.EventBuilder()
                                .setCategory(AnalyticHelper.CATEGORY_AREA_ID)
                                .setAction(AnalyticHelper.ACTION_ERROR)
                                .build());
                    }
                }, location);
        tokenFetcher.fetch();
    }

    private void submitLocationToRadioServer(String areaId) {
        String adId = AndroidUtil.getAdsId(context);
        if (location == null) location = AndroidUtil.getLastBestLocation(context);
        try {
            if (location != null) {
                String url = "http://radioserver.mienamthuc.com/api/loc/?area=" + URLEncoder.encode(areaId, "UTF-8")
                        + "&long=" + location.getLongitude()
                        + "&lat=" + location.getLatitude()
                        + "&AID=" + URLEncoder.encode(adId, "UTF-8");
                SimpleAppLog.info("Submit dump location to: " + url);
                FileUtils.copyURLToFile(new URL(url),
                        new File(fileHelper.getApplicationDir(), FileHelper.LOCATION_DUMP_API));
            }
        } catch (Exception e) {
            SimpleAppLog.error("Could not request to Radio Server location API", e);
        }

    }

    public String checkLocation(String ariaId) {
        String defaulLocations = "";
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(R.raw.radio_jp13_location);
            defaulLocations = IOUtils.toString(is);
        } catch (IOException e) {
            SimpleAppLog.error("Could not fetch radio jp13 location from resource", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {

                }
            }
        }
        Gson gson = new Gson();
        String address = AndroidUtil.findAddress(context, location);
        SimpleAppLog.info("Area name: " + address);
        if (address != null && address.length() > 0 && defaulLocations.length() > 0) {
            RadioLocationContainer container = gson.fromJson(defaulLocations, RadioLocationContainer.class);
            if (container != null) {
                List<RadioLocation> locations = container.getLocations();
                if (locations != null && locations.size() > 0) {
                    SimpleAppLog.info("Found " + locations.size() + " Radio location");
                    for (RadioLocation radioLocation : locations) {
                        if (address.toLowerCase().contains(radioLocation.getName().toLowerCase())) {
                            SimpleAppLog.info("Matched location: " + radioLocation.getName()
                                    + ". AreaID: " + radioLocation.getName());
                            return radioLocation.getAreaId();
                        }
                    }
                }
            }
        }
        return "";
    }

    public String findBestAreaId(String areaId) {
        if (setting.getTokenType() == Setting.TOKEN_TYPE_SERVER) {
            if (AndroidUtil.getLastBestLocation(context) != null) {
                return RadioArea.AREA_ID_TOKYO;
            } else {
                return "";
            }
        } else {
            return checkLocation(areaId);
        }
    }

    private void requestChannels(String token, String rawAreaId) {
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
            is = context.getResources().openRawResource(R.raw.default_channels);
            defaultChannel = IOUtils.toString(is);
        } catch (IOException e) {
            SimpleAppLog.error("Could not fetch default channels from resource", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {

                }
            }
        }

        RadioChannel radioChannel = null;
        try {
            SimpleAppLog.info("Load default channel: " + defaultChannel);
            SimpleAppLog.info("Raw area ID: " + rawAreaId);
            SimpleAppLog.info("Token: " + token);
            //rawAreaId = findBestAreaId(rawAreaId);
            if (AppDelegate.getInstance().isPremium()) {
                radioChannel = apiRequester.getChannels(defaultChannel);
            } else {
                radioChannel = apiRequester.getChannels(rawAreaId, setting.isRegion(), defaultChannel);
            }
            submitLocationToRadioServer(rawAreaId);
        } catch (IOException e) {
            SimpleAppLog.error("Could not get channel", e);
        }
        ChannelDBAdapter dbAdapter = new ChannelDBAdapter(context);
        try {
            dbAdapter.open();
            Collection<Channel> channels = dbAdapter.findByProvider(RadioProvider.RADIKO);
            if (radioChannel != null) {
                List<RadioChannel.Channel> channelList = radioChannel.getChannels();
                SimpleAppLog.info("Current Radiko channels: " + (channels == null ? "0" : channels.size()));
                SimpleAppLog.info("Found channels size: " + (channelList == null ? "0" : channelList.size()));
                if (channelList != null && channelList.size() > 0) {
                    dbAdapter.getDB().beginTransaction();
                    for (RadioChannel.Channel channel : channelList) {
                        Channel dbChannel = new Channel();
                        dbChannel.setName(channel.getName());
                        dbChannel.setType(channel.getService());
                        dbChannel.setUrl(channel.getStreamURL());
                        dbChannel.setKey(channel.getServiceChannelId());
                        dbChannel.setRadikoAreaID(channel.getRadikoAreaID());
                        dbChannel.setRegionID(channel.getRegionID());
                        SimpleAppLog.info("Start insert channel " + dbChannel.getName());
                        dbAdapter.insert(dbChannel);
                        if (channels != null && channels.contains(dbChannel)) {
                            SimpleAppLog.info("Remove same URL " + dbChannel.getUrl());
                            channels.remove(dbChannel);
                        }
                    }
                    dbAdapter.getDB().setTransactionSuccessful();
                    dbAdapter.getDB().endTransaction();
                }
            }
            if (channels != null && channels.size() > 0) {
                for (Channel channel : channels) {
                    SimpleAppLog.info("Delete unmatched Radiko channel " + channel.getName());
                    dbAdapter.delete(channel);
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
}
