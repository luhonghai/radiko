package com.dotohsoft.radio.data;

import com.dotohsoft.radio.api.APIRequester;

/**
 * Created by luhonghai on 3/10/15.
 */
public class RadioArea {

    public static final String AREA_ID_TOKYO = "JP13";

    private String id;

    private String provider;

    public String getProvider() {
        return provider;
    }

    public String getId() {
        return id;
    }

    private RadioArea() {
    }

    public static RadioArea getArea(String rawResponse, String provider) {
        return getArea(rawResponse, provider, null);
    }

    public static RadioArea getArea(String rawResponse, String provider, final APIRequester.RequesterListener requesterListener) {
        RadioArea area = new RadioArea();
        if (provider == null || rawResponse == null || rawResponse.trim().length() == 0)
            return null;
        if (requesterListener != null)
            requesterListener.onMessage("Validate raw response " + rawResponse + ". Provider: " + provider);
        area.provider = provider.toLowerCase();
        area.id = AREA_ID_TOKYO;
        if (rawResponse.length() > 0) {
            String rAID = "";
            for (int i = 47; i >= 1; i--) {
                String test = "JP" + Integer.toString(i);
                //if (requesterListener != null) requesterListener.onMessage("Test id: " + test);
                if (rawResponse.toLowerCase().contains(test.toLowerCase()) || rawResponse.equalsIgnoreCase(test)) {
                    rAID = "JP" + i;
                    break;
                }
            }
            if (rAID.length() > 0) {
                area.id = rAID;
            } else {
                if (rawResponse.equalsIgnoreCase("JP0")) {
                    area.id = "JP1";
                    return area;
                }
                if (area.provider.equalsIgnoreCase(RadioProvider.RADIKO))
                    return null;
            }
        } else if (area.provider.equalsIgnoreCase(RadioProvider.RADIKO)) {
            return null;
        }
        if (provider.equalsIgnoreCase(RadioProvider.NHK)) {
            String lId = area.id.substring(2, area.id.length());
            if (lId.length() == 1) {
                lId = "0" + lId + "0";
            } else if (lId.length() == 2) {
                lId = lId + "0";
            }
            area.id = lId;
        }
        return area;
    }
}
