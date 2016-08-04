package com.eaphone.common.datatables.samples.support;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.springframework.format.Formatter;

import com.eaphone.common.datatables.samples.model.DateParser;

/**
 * 处理 request param 中的 日期和时间
 * @author Xiaoyu Guo
 */
public class SpringDateFormatter implements Formatter<Date> {

    @Override
    public String print(Date object, Locale locale) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(object);
    }

    @Override
    public Date parse(String text, Locale locale) throws ParseException {
        return DateParser.parse(text);
    }

}
