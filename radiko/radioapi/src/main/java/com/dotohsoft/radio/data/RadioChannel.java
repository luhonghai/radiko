package com.dotohsoft.radio.data;

import java.util.List;

/**
 * Created by luhonghai on 3/10/15.
 */
public class RadioChannel {

    public static class Channel {
        private String service;
        private String serviceChannelId;
        private String name;
        private String streamURL;

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof Channel) {
                Channel dest = (Channel) obj;
                if (service == null || dest.service == null || serviceChannelId == null || dest.serviceChannelId == null) {
                    return false;
                }
                return service.equalsIgnoreCase(dest.service) && serviceChannelId.equalsIgnoreCase(dest.serviceChannelId);
            }
            return super.equals(obj);
        }

        public String getService() {
            if (service == null) return "";
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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStreamURL() {
            return streamURL;
        }

        public void setStreamURL(String streamURL) {
            this.streamURL = streamURL;
        }
    }

    private long serverTime;
    private long cachedTime;

    private String timezone;

    private List<Channel> channels;

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public long getCachedTime() {
        return cachedTime;
    }

    public void setCachedTime(long cachedTime) {
        this.cachedTime = cachedTime;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
