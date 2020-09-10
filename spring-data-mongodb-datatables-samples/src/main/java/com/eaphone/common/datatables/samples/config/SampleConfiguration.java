package com.eaphone.common.datatables.samples.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

@Configuration
public class SampleConfiguration {
	@Bean
	MongoServer mongoServer() {
		MongoServer server = new MongoServer(new MemoryBackend());
		server.bind();
		return server;
	}

	@Bean
	MongoClient mongoClient(MongoServer server) {
		return MongoClients
				.create(MongoClientSettings.builder()
						.applyToClusterSettings(
								builder -> builder.hosts(Arrays.asList(new ServerAddress(server.getLocalAddress()))))
						.build());
	}

	@Bean
	MongoTemplate mongoTemplate(MongoClient mongo) {
		MongoTemplate mt = new MongoTemplate(mongo, "test");
		return mt;
	}
}
