package com.gmail.radioserver2.data;

import java.util.List;

/**
 * Created by luhonghai on 4/23/15.
 */
public class GMapGeocodeResponse {

    public static class AddressComponent {

        public static final String TYPE_LOCALITY = "locality";

        public static final String TYPE_COUNTRY = "country";

        public static final String TYPE_ADMIN_AREA = "administrative_area";

        private String long_name;

        private String short_name;

        private List<String> types;

        public boolean isType(String type) {
            if (getTypes() != null && getTypes().size() > 0) {
                for (String t : getTypes()) {
                    if (t.toLowerCase().startsWith(type.toLowerCase())) {
                        return true;
                    }
                }
            }
            return  false;
        }

        public String getLong_name() {
            return long_name;
        }

        public void setLong_name(String long_name) {
            this.long_name = long_name;
        }

        public String getShort_name() {
            return short_name;
        }

        public void setShort_name(String short_name) {
            this.short_name = short_name;
        }

        public List<String> getTypes() {
            return types;
        }

        public void setTypes(List<String> types) {
            this.types = types;
        }
    }

    public static class Result {



        private String formatted_address;

        private List<AddressComponent> address_components;



        public String getFormatted_address() {
            return formatted_address;
        }

        public void setFormatted_address(String formatted_address) {
            this.formatted_address = formatted_address;
        }

        public List<AddressComponent> getAddress_components() {
            return address_components;
        }

        public void setAddress_components(List<AddressComponent> address_components) {
            this.address_components = address_components;
        }
    }

    private String status;

    private List<Result> results;

    public boolean isOk() {
        if (status == null || status.length() == 0) {
            return false;
        } else {
            return status.equalsIgnoreCase("ok");
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

}
