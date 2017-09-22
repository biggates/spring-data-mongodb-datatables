package com.eaphone.common.datatables.samples.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Some configuration for Jackson2
 * 
 * @author Xiaoyu Guo
 */
public class JacksonProductionObjectMapper extends ObjectMapper {
    private static final long serialVersionUID = -5352031618318757792L;

    public JacksonProductionObjectMapper() {
        super();

        this.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        this.setSerializationInclusion(Include.NON_NULL);

        this.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        this.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));

        this.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        SimpleModule m = new SimpleModule("EaphoneJsonDateModule", new Version(1, 0, 0, null, "com.eaphone", "spring-data-mongodb-datatables-samples"));
        m.addDeserializer(Date.class, new JacksonJsonDateDeserializer());
        this.registerModule(m);
    }
}
