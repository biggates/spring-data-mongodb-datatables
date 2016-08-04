package com.eaphone.common.datatables.samples.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateParser {

    private static String[] FORMATS = {
            // ISO 8601 date-time
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            // ISO 8601 date-time with time-secfrac
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            // 常用值
            "yyyy-MM-dd HH:mm:ss",
            // ISO 8601 full-date
            "yyyy-MM-dd" };

    public static Date parse(String text) throws ParseException {
        for (final String format : FORMATS) {
            final SimpleDateFormat sdf = new SimpleDateFormat(format);
            try {
                return sdf.parse(text);
            } catch (ParseException pe) {
                // continue to next format
            }
        }
        throw new ParseException("", 0);
    }

}
