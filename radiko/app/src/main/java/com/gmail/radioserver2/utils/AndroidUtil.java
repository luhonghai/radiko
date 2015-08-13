package com.gmail.radioserver2.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.Nullable;

import com.dotohsoft.radio.data.RadioProgram;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.GMapGeocodeResponse;
import com.gmail.radioserver2.data.Setting;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by luhonghai on 26/02/2015.
 */
public class AndroidUtil {

    private static final boolean DEBUG = false;

    private static String ADS_ID = "";

    public static void updateLanguage(final Context context) {
        updateLanguage(context, context.getResources().getString(R.string.default_language));
    }

    public static void updateTokenType(Context context, Location location) {
        Setting setting = new Setting(context);
        setting.load();
        String address = AndroidUtil.findAddress(context, location);
        if (address != null && address.length() > 0
                && (address.toLowerCase().contains("hanoi") || address.toLowerCase().contains("hà nội")
                || address.toLowerCase().contains("thanh hoa")
                || address.toLowerCase().contains("thanh hóa"))) {
            SimpleAppLog.info("Address is Hanoi. Set token type to server");
            setting.setTokenType(Setting.TOKEN_TYPE_SERVER);
        } else {
            setting.setTokenType(Setting.TOKEN_TYPE_CLIENT);
        }
        setting.save();
    }


    public static RadioProgram.Program getMaxTimedProgram(long startTime, long stopTime, List<RadioProgram.Program> listProgram) {
        if (listProgram == null || listProgram.size() == 0) {
            return null;
        } else {
            RadioProgram.Program insectStartProgram = null;
            RadioProgram.Program insectStopProgram = null;
            boolean changed = false;
            List<RadioProgram.Program> boundedProgramList = new ArrayList<>();
            for (RadioProgram.Program item : listProgram) {
                if (startTime >= item.getFromTime() && stopTime <= item.getToTime()) {
                    return item;
                } else {
                    if (startTime < item.getFromTime() && stopTime > item.getToTime()) {
                        boundedProgramList.add(item);
                    } else if (item.getFromTime() <= startTime && startTime <= item.getToTime() && item.getToTime() <= stopTime) {
                        insectStartProgram = item;
                    } else if (startTime <= item.getFromTime() && item.getFromTime() <= stopTime && stopTime <= item.getToTime()) {
                        insectStopProgram = item;
                    }
                }
            }

            RadioProgram.Program maxBoundedProgram = null;
            if (boundedProgramList.size() != 0) {
                maxBoundedProgram = boundedProgramList.get(0);
                for (RadioProgram.Program item : boundedProgramList) {
                    if ((item.getToTime() - item.getFromTime()) > (maxBoundedProgram.getToTime() - maxBoundedProgram.getFromTime())) {
                        maxBoundedProgram = item;
                    }
                }
            }
            if (maxBoundedProgram != null) {
                if (insectStartProgram != null) {
                    if ((insectStartProgram.getToTime() - startTime) > (maxBoundedProgram.getToTime() - maxBoundedProgram.getFromTime())) {
                        changed = true;
                        maxBoundedProgram = insectStartProgram;
                    }
                }

                if (insectStopProgram != null) {
                    if (changed) {
                        if ((stopTime - insectStopProgram.getFromTime()) > (insectStartProgram.getToTime() - startTime)) {
                            maxBoundedProgram = insectStopProgram;
                        }
                    } else {
                        if ((stopTime - insectStopProgram.getFromTime()) > (maxBoundedProgram.getToTime() - maxBoundedProgram.getFromTime())) {
                            maxBoundedProgram = insectStopProgram;
                        }
                    }
                }
                return maxBoundedProgram;
            } else {
                if (insectStartProgram == null && insectStopProgram == null) {
                    return null;
                } else if (insectStartProgram != null && insectStopProgram != null) {
                    if ((insectStartProgram.getToTime() - startTime) > (stopTime - insectStopProgram.getFromTime())) {
                        return insectStartProgram;
                    } else {
                        return insectStopProgram;
                    }
                } else if (insectStartProgram != null && insectStopProgram == null) {
                    return insectStartProgram;
                } else {
                    return insectStopProgram;
                }
            }
        }
    }


    public static void updateLanguage(final Context context, String locate) {
        Configuration c = new Configuration(context.getResources().getConfiguration());
        if (locate.equalsIgnoreCase("ja")) {
            c.locale = Locale.JAPANESE;
        } else if (locate.equalsIgnoreCase("en")) {
            c.locale = Locale.ENGLISH;
        } else {
            c.locale = new Locale(locate);
        }
        context.getResources().updateConfiguration(c, context.getResources().getDisplayMetrics());
    }

    // Do not call this function from the main thread. Otherwise,
    // an IllegalStateException will be thrown.
    public static String getAdsId(Context mContext) {
        if (ADS_ID != null && ADS_ID.length() > 0) {
            return ADS_ID;
        }
        Setting setting = new Setting(mContext);
        setting.load();
        if (setting.getAdsId() != null && setting.getAdsId().length() > 0) {
            ADS_ID = setting.getAdsId();
        }
        if (ADS_ID == null || ADS_ID.length() == 0) {
            AdvertisingIdClient.Info adInfo;
            try {
                adInfo = AdvertisingIdClient.getAdvertisingIdInfo(mContext);
                ADS_ID = adInfo == null ? "" : adInfo.getId();
                setting.setAdsId(ADS_ID);
                setting.save();
            } catch (Exception e) {
                // Google Play services is not available entirely.
                SimpleAppLog.error("Could not fetch Ads ID", e);
            }
        }
        return ADS_ID;
    }

    @Nullable
    public static Location getFakeLocation() {
        Location fakeLocation = new Location("fake");
        fakeLocation.setLongitude(142.95410156);
        fakeLocation.setLatitude(43.50075244);
        return fakeLocation;
    }

    public static Location filterLocation(Location location) {
//        if (DEBUG) {
//            return getFakeLocation();
//        }
        return location;
    }


    /**
     * @return the last know best location
     */
    public static Location getLastBestLocation(Context context) {
//        if (DEBUG) {
//            return getFakeLocation();
//        }
        LocationManager mLocationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        try {
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (locationGPS != null) return locationGPS;
            }
        } catch (Exception e) {
            SimpleAppLog.error("Could not get last known GPS location", e);
        }
        try {
            if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Location locationNet = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (locationNet != null) return locationNet;
            }
        } catch (Exception e) {
            SimpleAppLog.error("Could not get last known Network location", e);
        }
//        Location locationPassive = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
//        if (locationPassive != null) return  locationPassive;
        return null;
//
//        Location location;
//        long GPSLocationTime = 0;
//        if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }
//        long NetLocationTime = 0;
//        if (null != locationNet) {
//            NetLocationTime = locationNet.getTime();
//        }
//        if ( 0 < GPSLocationTime - NetLocationTime ) {
//            location =  locationGPS != null ? locationGPS : locationNet;
//        }
//        else {
//            location = locationNet != null ? locationNet : locationGPS;
//        }
//        if (location == null)
//            return locationPassive;
//        return location;
    }

    public static String findAddress(Context context, Location prettyLocation) {
        Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
        Location location = prettyLocation != null ? prettyLocation : getLastBestLocation(context);
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
                SimpleAppLog.error("Could not fetch address", e);
            }
        } else {
            SimpleAppLog.info("No location data found");
        }
        return "";
    }


    public static List<Address> getFromLocation(double lat, double lng, int maxResult) {
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
}
