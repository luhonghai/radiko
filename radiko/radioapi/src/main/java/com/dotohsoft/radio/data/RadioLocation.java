package com.dotohsoft.radio.data;

/**
 * Created by luhonghai on 4/20/15.
 */
public class RadioLocation {

    private String name;
    private double lon;
    private double lat;
    private double radius;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}
