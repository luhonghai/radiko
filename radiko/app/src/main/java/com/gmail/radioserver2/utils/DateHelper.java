package com.gmail.radioserver2.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by luhonghai on 25/02/2015.
 */
public class DateHelper {
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static Date convertStringToDate(String date) {
        try {
            if (date == null || date.length() == 0) return null;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.US);
            return simpleDateFormat.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String convertDateToString(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.US);
        return simpleDateFormat.format(date);
    }
}
