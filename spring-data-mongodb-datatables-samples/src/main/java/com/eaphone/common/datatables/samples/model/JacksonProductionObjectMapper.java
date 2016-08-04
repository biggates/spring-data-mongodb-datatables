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
 * 统一定义 JSON 输出的格式，如 时间戳统一定义为 ISO 8601 格式
 * 
 * @author Xiaoyu Guo
 */
public class JacksonProductionObjectMapper extends ObjectMapper {
    private static final long serialVersionUID = -5352031618318757792L;

    public JacksonProductionObjectMapper() {
        super();

        // 除 map 外每个字段按字母顺序输出
        this.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        // null 值也输出
        this.setSerializationInclusion(Include.NON_NULL);

        // 默认不将日期输出为 UNIX 时间戳
        this.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // 默认的时间格式 （ISO-8601）
        this.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));

        // 默认的时区（东8区）
        this.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        SimpleModule m = new SimpleModule("EaphoneJsonDateModule", new Version(1, 0, 0, null, "com.eaphone.jiankang", "jiankang-playground"));
        m.addDeserializer(Date.class, new JacksonJsonDateDeserializer());
        this.registerModule(m);
    }
}
