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
        RadioArea area = new RadioArea();
        area.provider = provider.toLowerCase();
        area.id = RadioArea.AREA_ID_TOKYO;
        if (rawResponse != null && rawResponse.length() > 0) {
            rawResponse = rawResponse.replace("\n", " ");
            while (rawResponse.contains("  ")) {
                rawResponse = rawResponse.replace("  ", " ");
            }
            rawResponse = rawResponse.trim();
            if (rawResponse.equalsIgnoreCase("out")) rawResponse = RadioArea.AREA_ID_TOKYO;
            String rAID;
            if (rawResponse.contains(",")) {
                rAID = rawResponse.split(",")[0].trim();
            } else {
                rAID = rawResponse;
            }
            area.id = rAID;
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
