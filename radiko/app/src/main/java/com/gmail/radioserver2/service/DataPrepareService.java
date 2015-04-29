package com.gmail.radioserver2.service;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
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
import com.gmail.radioserver2.data.GMapGeocodeResponse;
import com.gmail.radioserver2.data.Setting;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.utils.AndroidUtil;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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
        setting.load();
    }

    public void execute() {
        SimpleAppLog.info("Start data prepare service");
        // Get tracker.
        final Tracker t = AnalyticHelper.getTracker(context);
        String address = findAddress();
        if (address != null && address.length() > 0
                && (address.toLowerCase().contains("hanoi") || address.toLowerCase().contains("hà nội")
                || address.toLowerCase().contains("thanh hoa")
                || address.toLowerCase().contains("thanh hóa"))) {
            SimpleAppLog.info("Address is Hanoi. Set token type to server");
            setting.setTokenType(Setting.TOKEN_TYPE_SERVER);
            setting.save();
        }

        TokenFetcher tokenFetcher = TokenFetcher.getTokenFetcher(context, new TokenFetcher.OnTokenListener() {
            @Override
            public void onTokenFound(String token, String rawAreaId) {
                requestChannels(rawAreaId);
                t.send(new HitBuilders.EventBuilder()
                        .setCategory(AnalyticHelper.CATEGORY_AREA_ID)
                        .setAction(rawAreaId)
                        .build());
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
        Location locationPassive = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

        Location location;
        long GPSLocationTime = 0;
        if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }
        long NetLocationTime = 0;
        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }
        if ( 0 < GPSLocationTime - NetLocationTime ) {
            location =  locationGPS != null ? locationGPS : locationNet;
        }
        else {
            location = locationNet != null ? locationNet : locationGPS;
        }
        if (location == null)
            return locationPassive;
        return location;
    }

    private String findAddress() {
        Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
        Location location = getLastBestLocation();
        if (location != null) {
            try {
                SimpleAppLog.info("Start find address by location");
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(
                            //35.439860, 139.342154
                            location.getLatitude(), location.getLongitude(),
                            1);
                } catch (Exception e) {
                    SimpleAppLog.error("Could not fetch address from Google API in Android", e);
                }
                if (addresses == null || addresses.size() == 0) {
                    try {
                        addresses = getFromLocation(
                                location.getLatitude(), location.getLongitude()
                                //35.439860, 139.342154
                                , 1);
                    } catch (Exception e) {
                        SimpleAppLog.error("Could not fetch address from Google API Remote", e);
                    }
                }
                if (addresses != null && addresses.size() > 0) {
                    Address address = addresses.get(0);
                    Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
                    SimpleAppLog.info("Address: " + gson.toJson(address));
                    String area = address.getAdminArea();
                    if (area == null || area.length() == 0) {
                        area = address.getLocality();
                    }
                    return area;
                }
            } catch (Exception e) {
                SimpleAppLog.error("Could not fetch address",e);
            }
        } else {
            SimpleAppLog.info("No location data found");
        }
        return "";
    }

    private void submitLocationToRadioServer(String areaId) {
        String adId = AndroidUtil.getAdsId(context);
        Location location = getLastBestLocation();
        try {
            if (location != null) {
                String url = "http://radioserver.mienamthuc.com/api/loc/?area=" + URLEncoder.encode(areaId,"UTF-8")
                        + "&long=" + location.getLongitude()
                        + "&lat=" + location.getLatitude()
                        + "&AID=" + URLEncoder.encode(adId, "UTF-8");
                SimpleAppLog.info("Submit dump location to: " + url);
                FileUtils.copyURLToFile(new URL(url),
                        new File(fileHelper.getApplicationDir(), FileHelper.LOCATION_DUMP_API));
            }
        } catch (Exception e) {
            SimpleAppLog.error("Could not request to Radio Server location API",e);
        }

    }


    private List<Address> getFromLocation(double lat, double lng, int maxResult){
        String address = String.format(Locale.ENGLISH, "http://maps.googleapis.com/maps/api/geocode/json?latlng=%1$f,%2$f&sensor=true&language=" + Locale.ENGLISH.getCountry(), lat, lng);
        HttpGet httpGet = new HttpGet(address);
        HttpClient client = new DefaultHttpClient();
        HttpResponse response;
        List<Address> retList = null;

        try {
            response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            String rawData = IOUtils.toString(stream, "UTF-8");

            SimpleAppLog.info("Raw address data: " + rawData);
            if (rawData != null && rawData.length() > 0) {
                Gson gson = new Gson();
                GMapGeocodeResponse gmapRes = gson.fromJson(rawData, GMapGeocodeResponse.class);
                retList = new ArrayList<Address>();
                if (gmapRes.isOk()) {
                    List<GMapGeocodeResponse.Result> results = gmapRes.getResults();
                    if (results != null && results.size() > 0) {
                        for (int i = 0; i < maxResult; i++) {
                            GMapGeocodeResponse.Result result = results.get(i);
                            Address addr = new Address(Locale.ENGLISH);
                            addr.setAddressLine(0, result.getFormatted_address());
                            List<GMapGeocodeResponse.AddressComponent> addressComponents = result.getAddress_components();
                            if (addressComponents != null && addressComponents.size() > 0) {
                                for (GMapGeocodeResponse.AddressComponent addressComponent : addressComponents) {
                                    if (addressComponent.isType(GMapGeocodeResponse.AddressComponent.TYPE_ADMIN_AREA)) {
                                        addr.setAdminArea(addressComponent.getLong_name());
                                    } else if (addressComponent.isType(GMapGeocodeResponse.AddressComponent.TYPE_COUNTRY)) {
                                        addr.setCountryName(addressComponent.getLong_name());
                                    } else if (addressComponent.isType(GMapGeocodeResponse.AddressComponent.TYPE_LOCALITY)) {
                                        addr.setLocality(addressComponent.getLong_name());
                                    }
                                }
                            }
                            retList.add(addr);
                        }
                    }
                }
            }

        } catch (ClientProtocolException e) {
            SimpleAppLog.error("Error calling Google geocode webservice.", e);
        } catch (IOException e) {
            SimpleAppLog.error("Error calling Google geocode webservice.", e);
        } catch (Exception e) {
            SimpleAppLog.error("Error parsing Google geocode webservice response.", e);
        }

        return retList;
    }


    public String checkLocation(String ariaId) {
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
        String address = findAddress();
        SimpleAppLog.info("Area name: " + address);
        if (address != null && address.length() > 0 && defaulLocations.length() > 0) {
            RadioLocationContainer container = gson.fromJson(defaulLocations, RadioLocationContainer.class);
            if (container != null) {
                List<RadioLocation> locations = container.getLocations();
                if (locations != null && locations.size() > 0) {
                    SimpleAppLog.info("Found " + locations.size() + " Radio location");
                    for(RadioLocation radioLocation : locations) {
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
            if (getLastBestLocation() != null) {
                return RadioArea.AREA_ID_TOKYO;
            } else {
                return "";
            }
        } else {
            return checkLocation(areaId);
        }
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

        RadioChannel radioChannel = null;
        try {
            SimpleAppLog.info("Load default channel: " + defaultChannel);
            SimpleAppLog.info("Raw area ID: " + rawAreaId);
            rawAreaId = findBestAreaId(rawAreaId);
            radioChannel = apiRequester.getChannels(rawAreaId, setting.isRegion(), defaultChannel);
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
