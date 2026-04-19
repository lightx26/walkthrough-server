package com.pet.walkthroughserver.modules.search.infra;

import java.time.Duration;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

@Configuration
public class ElasticsearchClientConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchProperties properties) {
        Duration connectTimeout = Duration.parse("PT" + properties.getConnectTimeout().toUpperCase());
        Duration readTimeout = Duration.parse("PT" + properties.getReadTimeout().toUpperCase());

        RestClient restClient = RestClient.builder(HttpHost.create(properties.getUris()))
                .setRequestConfigCallback(config -> config
                        .setConnectTimeout((int) connectTimeout.toMillis())
                        .setSocketTimeout((int) readTimeout.toMillis()))
                .build();

        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
