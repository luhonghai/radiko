package com.dotohsoft.radio.api;

import com.dotohsoft.radio.Constant;
import com.dotohsoft.radio.data.RadioArea;
import com.dotohsoft.radio.data.RadioChannel;
import com.dotohsoft.radio.data.RadioLocation;
import com.dotohsoft.radio.data.RadioProgram;
import com.dotohsoft.radio.data.RadioProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by luhonghai on 3/10/15.
 */

public class APIRequester {
    private final File cachedFolder;
    private Date now;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH", Locale.JAPAN);

    public static interface RequesterListener {
        public void onMessage(String message);
        public void onError(String error, Throwable throwable);
    }

    private RequesterListener requesterListener;

    public void setRequesterListener(RequesterListener requesterListener) {
        this.requesterListener = requesterListener;
    }

    public APIRequester(File cachedFolder) {
        this.cachedFolder = cachedFolder;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+9"));
        now = cal.getTime();
    }

    public APIRequester(File cachedFolder, Date now) {
        this.cachedFolder = cachedFolder;
        this.now = now;
    }

    public void addDay(int day) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+9"));
        cal.setTime(now);
        cal.add(Calendar.DATE, day);
        now = cal.getTime();
    }

    public RadioChannel getChannels(String rawAreaId, boolean isRegion, String defaultChannelJsonSource) throws IOException {
        RadioChannel radioChannel;
        Gson gson = new Gson();
        if (defaultChannelJsonSource != null && defaultChannelJsonSource.length() > 0) {
            radioChannel = gson.fromJson(defaultChannelJsonSource, RadioChannel.class);
        } else {
            radioChannel = new RadioChannel();
        }

        List<RadioChannel.Channel> channels = radioChannel.getChannels();
        if (channels == null) {
            channels = new ArrayList<RadioChannel.Channel>();
        }

        getRadikoChannels(channels, RadioArea.getArea(rawAreaId, RadioProvider.RADIKO, requesterListener));
        if (!isRegion) {
            if (requesterListener != null) requesterListener.onMessage("Region is disabled. Try to fetch channel from JP13");
            getRadikoChannels(channels, RadioArea.getArea(RadioArea.AREA_ID_TOKYO, RadioProvider.RADIKO, requesterListener));
        }

        radioChannel.setChannels(channels);
        return radioChannel;
    }

    public void getRadikoChannels(final List<RadioChannel.Channel> channels,final RadioArea area) throws IOException {
        if (area != null && area.getId() != null && area.getId().length() > 0) {
            try {
                File cachedXml = new File(cachedFolder, "channel_" + area.getProvider() + "_" + area.getId() + "_" + sdf.format(now) + ".xml");
                if (requesterListener != null) requesterListener.onMessage("Cached file: " + cachedXml.getAbsolutePath());
                if (!cachedXml.exists()) {
                    FileUtils.copyURLToFile(new URL("http://radiko.jp/v2/station/list/" + area.getId() + ".xml"), cachedXml);
                    if (cachedXml.exists() && !FileUtils.readFileToString(cachedXml, "UTF-8").toLowerCase().contains("stations")) {
                        FileUtils.forceDelete(cachedXml);
                    }
                }
                if (cachedXml.exists()) {
                    Document doc = Jsoup.parse(cachedXml, "UTF-8");
                    Elements root = doc.getElementsByTag("stations");
                    if (root != null && root.size() > 0) {
                        if (requesterListener != null) requesterListener.onMessage(area.getId() + " | " + root.get(0).attr("area_name"));
                    }
                    Elements stations = doc.getElementsByTag("station");
                    if (stations != null && stations.size() > 0) {
                        for (int i = 0; i < stations.size(); i++) {
                            Element station = stations.get(i);
                            RadioChannel.Channel channel = new RadioChannel.Channel();
                            channel.setService(area.getProvider());
                            channel.setServiceChannelId(station.getElementsByTag("id").text());
                            channel.setName(station.getElementsByTag("name").text());
                            channel.setStreamURL("rtmpe://f-radiko.smartstream.ne.jp/" + channel.getServiceChannelId() + "/_definst_/simul-stream.stream");
                            if (!channels.contains(channel)) {
                                channels.add(channel);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (requesterListener != null)
                    requesterListener.onError("Could not fetch channels",e);
            }
        } else {
            if (requesterListener != null) requesterListener.onMessage("No area ID found!");
        }
    }

    public RadioProgram getPrograms(RadioChannel.Channel channel, RadioArea area, String adID) throws IOException {
        if (area == null || area.getId() == null || area.getId().length() == 0 || channel == null) return null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strCachedFile = "program_" + area.getProvider() + "_" + channel.getServiceChannelId() + "_" + area.getId() + "_" + sdf.format(now) + ".json";

        File cachedFile = new File(cachedFolder, strCachedFile);

        if (!cachedFile.exists()) {
            String requesturl = Constant.ROOT_API_URL  + Constant.API_PROGRAM
                    + "?" + Constant.ARG_PROVIDER + "=" + area.getProvider()
                    + "&" + Constant.ARG_CHANNEL + "=" + URLEncoder.encode(channel.getServiceChannelId(), "UTF-8")
                    + "&" + Constant.ARG_DATE + "=" + URLEncoder.encode(simpleDateFormat.format(now),"UTF-8")
                    + "&" + Constant.ARG_AREA + "=" + URLEncoder.encode(area.getId(),"UTF-8")
                    + "&AID=" + URLEncoder.encode(adID, "UTF-8");
            URL url =new URL(requesturl);

            if (requesterListener != null) requesterListener.onMessage("Request program url: " + requesturl);
                FileUtils.copyURLToFile(url, cachedFile);
                if (cachedFile.exists()) {
                    String source = FileUtils.readFileToString(cachedFile, "UTF-8");
                    if (source == null || !source.toLowerCase().contains("cachedtime")) {
                        try {
                            FileUtils.forceDelete(cachedFile);
                        } catch (Exception e) {

                        }
                    }
                }
        }
        if (cachedFile.exists()) {
            Gson gson = new Gson();
            String rawJson = FileUtils.readFileToString(cachedFile, "UTF-8");
            RadioProgram program = gson.fromJson(rawJson, RadioProgram.class);

            return program;
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        APIRequester requester = new APIRequester(FileUtils.getTempDirectory());
        RadioChannel.Channel channel = new RadioChannel.Channel();
        channel.setService(RadioProvider.RADIKO);
        channel.setServiceChannelId("TBS");
        RadioArea area = RadioArea.getArea("JP13,東京都,tokyo Japan", RadioProvider.RADIKO);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        final List<RadioLocation> locations = new ArrayList<RadioLocation>();

        requester.setRequesterListener(new RequesterListener() {
            @Override
            public void onMessage(String message) {
                if (message.contains("JAPAN")) {
                    RadioLocation location = new RadioLocation();
                    String[] data = message.split("\\|");
                    location.setAreaId(data[0].trim());
                    String name = data[1];
                    if (name.endsWith("JAPAN")) {
                        name = name.substring(0, name.length() - "JAPAN".length());
                    }
                    location.setName(name.trim());
                    locations.add(location);
                }
                System.out.println(message);
            }

            @Override
            public void onError(String error, Throwable throwable) {
                System.out.println("Error: " + error);
                throwable.printStackTrace();
            }
        });
        try {

            for (int i = 1; i <= 47; i++) {
                System.out.println("Test #" + i);
                requester.getChannels(RadioArea.getArea("JP" + i, RadioProvider.RADIKO).getId(), true, null);
            }

            //System.out.print(gson.toJson(radioProgram));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(gson.toJson(locations));
//        try {
//            System.out.println("========================");
//            RadioChannel radioChannel = requester.getChannels("JP40", true, "");
//            System.out.print(gson.toJson(radioChannel));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String playUrl = "rtmpe://f-radiko.smartstream.ne.jp/TBS/_definst_/simul-stream.stream";
//
//            int port = 1935;
//            String tcUrl = playUrl.substring(0, playUrl.lastIndexOf("/") - 1);
//        System.out.println("Update tcURL to " + tcUrl);
//
//
//            playUrl = "rtmp://127.0.0.1:" + port + playUrl.substring("rtmpe://f-radiko.smartstream.ne.jp".length(), playUrl.length());
//        System.out.println(playUrl);

    }
}
