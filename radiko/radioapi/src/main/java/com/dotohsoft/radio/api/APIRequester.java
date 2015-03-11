package com.dotohsoft.radio.api;

import com.dotohsoft.radio.Constant;
import com.dotohsoft.radio.data.RadioArea;
import com.dotohsoft.radio.data.RadioChannel;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by luhonghai on 3/10/15.
 */

public class APIRequester {
    private final File cachedFolder;
    private final Date now = new Date(System.currentTimeMillis());
    private final SimpleDateFormat sdf = new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT, Locale.JAPAN);

    public APIRequester(File cachedFolder) {
        this.cachedFolder = cachedFolder;
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
        getRadikoChannels(channels, RadioArea.getArea(rawAreaId, RadioProvider.RADIKO));
        if (isRegion)
            getRadikoChannels(channels, RadioArea.getArea(RadioArea.AREA_ID_TOKYO, RadioProvider.RADIKO));

        radioChannel.setChannels(channels);
        return radioChannel;
    }

    public void getRadikoChannels(final List<RadioChannel.Channel> channels,final RadioArea area) throws IOException {
        File cachedXml = new File(cachedFolder, "channel_" + area.getProvider() + "_" + area.getId() + "_" + sdf.format(now) + ".xml");
        if (!cachedXml.exists()) {
            FileUtils.copyURLToFile(new URL("http://radiko.jp/v2/station/list/" + area.getId() + ".xml"), cachedXml);
            if (cachedXml.exists() && !FileUtils.readFileToString(cachedXml, "UTF-8").toLowerCase().contains("stations")) {
                FileUtils.forceDelete(cachedXml);
            }
        }
        if (cachedXml.exists()) {
            Document doc = Jsoup.parse(cachedXml, "UTF-8");
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
    }

    public RadioProgram getPrograms(RadioChannel.Channel channel, RadioArea area) throws IOException {


        String strCachedFile = "program_" + area.getProvider() + "_" + channel.getServiceChannelId() + "_" + area.getId() + "_" + sdf.format(now) + ".json";
        File cachedFile = new File(cachedFolder, strCachedFile);
        if (!cachedFile.exists()) {
            URL url =new URL(Constant.ROOT_API_URL + "/" + Constant.API_PROGRAM
                                + "?" + Constant.ARG_PROVIDER + "=" + area.getProvider()
                                + "&" + Constant.ARG_CHANNEL + "=" + channel.getServiceChannelId()
                                + "&" + Constant.ARG_AREA + "=" + area.getId());
            FileUtils.copyURLToFile(url, cachedFile);
            if (cachedFile.exists() && FileUtils.readFileToString(cachedFile, "UTF-8").toLowerCase().contains("error:")) {
                FileUtils.forceDelete(cachedFile);
            }
        }
        if (cachedFile.exists()) {
            Gson gson = new Gson();
            String rawJson = FileUtils.readFileToString(cachedFile, "UTF-8");
            return gson.fromJson(rawJson, RadioProgram.class);
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
//        APIRequester requester = new APIRequester(FileUtils.getTempDirectory());
//        RadioChannel.Channel channel = new RadioChannel.Channel();
//        channel.setService(RadioProvider.RADIKO);
//        channel.setServiceChannelId("TBS");
//        RadioArea area = RadioArea.getArea("JP13,東京都,tokyo Japan", RadioProvider.RADIKO);
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        try {
//            RadioProgram radioProgram = requester.getPrograms(channel, area);
//
//            System.out.print(gson.toJson(radioProgram));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            System.out.println("========================");
//            RadioChannel radioChannel = requester.getChannels("JP40", true, "");
//            System.out.print(gson.toJson(radioChannel));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        String playUrl = "rtmpe://f-radiko.smartstream.ne.jp/TBS/_definst_/simul-stream.stream";

            int port = 1935;
            String tcUrl = playUrl.substring(0, playUrl.lastIndexOf("/") - 1);
        System.out.println("Update tcURL to " + tcUrl);


            playUrl = "rtmp://127.0.0.1:" + port + playUrl.substring("rtmpe://f-radiko.smartstream.ne.jp".length(), playUrl.length());
        System.out.println(playUrl);

    }
}
