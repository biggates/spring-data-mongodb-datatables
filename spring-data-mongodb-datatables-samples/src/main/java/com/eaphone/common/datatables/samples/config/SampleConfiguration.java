package com.eaphone.common.datatables.samples.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;

@Configuration
public class SampleConfiguration {
    @Bean
    Fongo fongo() {
        return new Fongo("InMemoryMongo");
    }
    
    @Bean
    MongoClient mongoClient(Fongo fongo) {
        return fongo.getMongo();
    }
    
    @Bean
    MongoTemplate mongoTemplate(MongoClient mongo) {
        MongoTemplate mt = new MongoTemplate(mongo, "test");
        return mt;
    }
}
