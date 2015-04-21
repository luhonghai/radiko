package com.gmail.radioserver2.service;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

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
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.utils.StringUtil;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.games.Game;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
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

    public DataPrepareService(Context context) {
        this.context = context;
        fileHelper = new FileHelper(context);
        setting = new Setting(context);
    }

    public void execute() {
        // Get tracker.
        final Tracker t = AnalyticHelper.getTracker(context);

        setting.load();
        TokenFetcher tokenFetcher = TokenFetcher.getTokenFetcher(context, new TokenFetcher.OnTokenListener() {
            @Override
            public void onTokenFound(String token, String rawAreaId) {
                String areaId = StringUtil.escapeJapanSpecialChar(rawAreaId);
                if (areaId != null && areaId.length() > 0) {
                    areaId = areaId.replace("\n", " ");
                    areaId = areaId.replace("\t", " ");
                    while (areaId.contains("  ")) {
                        areaId = areaId.replace("  ", " ");
                    }
                    areaId = areaId.trim();
                    requestChannels(areaId);
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory(AnalyticHelper.CATEGORY_AREA_ID)
                            .setAction(areaId)
                            .build());
                } else {
                    requestChannels("");
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory(AnalyticHelper.CATEGORY_AREA_ID)
                            .setAction(AnalyticHelper.ACTION_NULL)
                            .build());
                }
            }

            @Override
            public void onError(String message, Throwable throwable) {
                SimpleAppLog.error(message, throwable);
                //requestChannels("");
                t.send(new HitBuilders.EventBuilder()
                        .setCategory(AnalyticHelper.CATEGORY_AREA_ID)
                        .setAction(AnalyticHelper.ACTION_ERROR)
                        .build());
            }
        });
        tokenFetcher.fetch();
    }

    /**
     * @return the last know best location
     */
    private Location getLastBestLocation() {
        LocationManager mLocationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        Location locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if ( 0 < GPSLocationTime - NetLocationTime ) {
            return locationGPS;
        }
        else {
            return locationNet;
        }
    }

    private String checkLocation(String ariaId) {
        String defaulLocations = "";
        InputStream is = null;
        try {
            is =context.getResources().openRawResource(R.raw.radio_jp13_location);
            defaulLocations = IOUtils.toString(is);
        } catch (IOException e) {
            SimpleAppLog.error("Could not fetch radio jp13 location from resource",e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {

                }
            }
        }
        Gson gson = new Gson();
        Location location = getLastBestLocation();
        if (location != null && defaulLocations.length() > 0) {
            SimpleAppLog.info("Current location. Lat: " + location.getLatitude() + ". Lon: " + location.getLongitude());
            RadioLocationContainer container = gson.fromJson(defaulLocations, RadioLocationContainer.class);
            if (container != null) {
                List<RadioLocation> locations = container.getLocations();
                if (locations != null && locations.size() > 0) {
                    SimpleAppLog.info("Found " + locations.size() + " Radio location JP13");
                    boolean valid = false;
                    for(RadioLocation radioLocation : locations) {
                        float[] result = new float[1];
                        Location.distanceBetween(radioLocation.getLat(), radioLocation.getLon(),
                                location.getLatitude(), location.getLongitude(),
                                //36.863084, 139.283261,
                                result);
                        float distance = result[0];
                        SimpleAppLog.info("Distance between current location and " + radioLocation.getName() + " is " + distance + "m");
                        if (distance <= radioLocation.getRadius()) {
                            valid = true;
                            SimpleAppLog.info("Valid radius " + radioLocation.getRadius() + ". Area: " + radioLocation.getName());
                            break;
                        }
                    }
                    if (valid) {
                        return RadioArea.AREA_ID_TOKYO;
                    } else {
                        if (ariaId.toLowerCase().contains(RadioArea.AREA_ID_TOKYO.toLowerCase())) {
                            // Not in JP13
                            return "";
                        }
                    }
                }
            }
        }
        return ariaId;
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
                rawAreaId = checkLocation(rawAreaId);
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
