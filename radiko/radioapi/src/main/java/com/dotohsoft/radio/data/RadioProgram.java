package com.dotohsoft.radio.data;

import java.util.List;

/**
 * Created by luhonghai on 3/10/15.
 */

public class RadioProgram {

    public static class Program {

        private String title;

        private String subTitle;

        private String information;

        private String description;

        private long fromTime;

        private long toTime;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSubTitle() {
            return subTitle;
        }

        public void setSubTitle(String subTitle) {
            this.subTitle = subTitle;
        }

        public String getInformation() {
            return information;
        }

        public void setInformation(String information) {
            this.information = information;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public long getFromTime() {
            return fromTime;
        }

        public void setFromTime(long fromTime) {
            this.fromTime = fromTime;
        }

        public long getToTime() {
            return toTime;
        }

        public void setToTime(long toTime) {
            this.toTime = toTime;
        }
    }

    private long cachedTime;

    private long serverTime;

    private String channelId;

    private String service;

    private String serviceChannelId;

    private String date;

    private String timezone;

    private List<Program> programs;

    public long getCachedTime() {
        return cachedTime;
    }

    public void setCachedTime(long cachedTime) {
        this.cachedTime = cachedTime;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getServiceChannelId() {
        return serviceChannelId;
    }

    public void setServiceChannelId(String serviceChannelId) {
        this.serviceChannelId = serviceChannelId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public List<Program> getPrograms() {
        return programs;
    }

    public void setPrograms(List<Program> programs) {
        this.programs = programs;
    }

}
