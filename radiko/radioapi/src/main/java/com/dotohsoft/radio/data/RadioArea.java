package com.dotohsoft.radio.data;

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

    private RadioArea() {}

    public static RadioArea getArea(String rawResponse, String provider) {
        if (rawResponse != null && rawResponse.length() > 0) {
            rawResponse = rawResponse.replace("\n", " ");
            while (rawResponse.contains("  ")) {
                rawResponse = rawResponse.replace("  ", " ");
            }
            rawResponse = rawResponse.trim();
            RadioArea area = new RadioArea();
            area.provider = provider.toLowerCase();
            String rAID;
            if (rawResponse.contains(",")) {
                rAID = rawResponse.split(",")[0].trim();
            } else {
                rAID = rawResponse;
            }
            if (provider.equalsIgnoreCase(RadioProvider.RADIKO)) {
                area.id = rAID;
            } else if (provider.equalsIgnoreCase(RadioProvider.NHK)) {
                String lId = rAID.substring(2, rAID.length());
                if (lId.length() == 1) {
                    lId = "0" + lId + "0";
                } else if (lId.length() == 2) {
                    lId = lId + "0";
                } else {
                    return null;
                }
                area.id = lId;
            } else {
                return null;
            }
            return area;
        }
        return null;
    }
}
