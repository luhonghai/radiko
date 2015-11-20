package com.dotohsoft.rtmpdump;

public class RTMP {
    public static int MESSAGE_ERROR = 0;
    public static int MESSAGE_INFO = 1;
    public static int MESSAGE_DEBUG = 2;

    static {
//        System.loadLibrary("crypto");
//        System.loadLibrary("ssl");
        System.loadLibrary("rtmp");
        System.loadLibrary("rtmpdump");
    }

    public static void onMessage(String type, String message) {

    }

    public native void init(String token, String dest);

    public native void stop();
}
