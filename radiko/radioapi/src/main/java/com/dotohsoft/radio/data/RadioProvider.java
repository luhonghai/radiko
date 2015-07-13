package com.dotohsoft.radio.data;

/**
 * Created by luhonghai on 3/10/15.
 */
public class RadioProvider {

    public static final String NHK = "nhk";

    public static final String RADIKO = "radiko";

    private String name;

    public String getName() {
        return name;
    }

    public static RadioProvider getProvider(String name) {
        if (name != null && name.length() > 0 && (name.equalsIgnoreCase(NHK) || name.equalsIgnoreCase(RADIKO))) {
            RadioProvider provider = new RadioProvider();
            provider.name = name.toLowerCase();
            return provider;
        }
        return null;
    }

    private RadioProvider() {}
}
