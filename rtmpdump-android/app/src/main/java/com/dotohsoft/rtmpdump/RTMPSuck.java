package com.dotohsoft.rtmpdump;

public class RTMPSuck {
    static {
        System.loadLibrary("rtmp");
        System.loadLibrary("rtmpsuck");
    }
    public native void init(String token, String dest);
    public native void stop();
}
