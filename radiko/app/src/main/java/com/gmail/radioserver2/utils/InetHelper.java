package com.gmail.radioserver2.utils;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by luhonghai on 3/11/15.
 */
public class InetHelper {

    public static boolean isPortOpen(final String ip, final int port, final int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        }
        catch(ConnectException ce){
            //SimpleAppLog.error("Could not connect to " + ip + ":" + port,ce);
            return false;
        }
        catch (Exception ex) {
            //SimpleAppLog.error("Could not connect to " + ip + ":" + port,ex);
            return false;
        }
    }
}
