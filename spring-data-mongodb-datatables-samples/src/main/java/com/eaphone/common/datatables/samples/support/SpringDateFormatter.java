package com.eaphone.common.datatables.samples.support;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * Handle format and parse of {@link Date}
 * @author Xiaoyu Guo
 */
public class SpringDateFormatter implements Formatter<Date> {

    /* (non-Javadoc)
     * @see org.springframework.format.Printer#print(java.lang.Object, java.util.Locale)
     */
    @Override
    public String print(Date object, Locale locale) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(object);
    }

    /* (non-Javadoc)
     * @see org.springframework.format.Parser#parse(java.lang.String, java.util.Locale)
     */
    @Override
    public Date parse(String text, Locale locale) throws ParseException {
        return DateParser.parse(text);
    }

}
