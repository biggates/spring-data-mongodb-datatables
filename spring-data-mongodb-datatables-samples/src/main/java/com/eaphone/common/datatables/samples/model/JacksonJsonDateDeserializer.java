package com.eaphone.common.datatables.samples.model;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * 对常见的几种日期格式做统一处理
 * 
 * @author Xiaoyu Guo
 */
public class JacksonJsonDateDeserializer extends StdDeserializer<Date> {
    private static final long serialVersionUID = -8548748138501821414L;

    public JacksonJsonDateDeserializer() {
        super(Date.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser,
     * com.fasterxml.jackson.databind.DeserializationContext)
     */
    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        if (p.getCurrentToken().isNumeric()) {
            // 这里是 timestamp
            try {
                return new Date(p.getLongValue());
            } catch (Exception nfe) {
                // continue to next format
            }
        } else {
            final String text = p.getText();
            try {
                return DateParser.parse(text);
            } catch (ParseException pe) {
                return null;
            }
        }
        return null;
    }

}
