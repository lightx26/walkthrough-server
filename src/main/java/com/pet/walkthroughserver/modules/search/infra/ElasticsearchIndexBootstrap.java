package com.pet.walkthroughserver.modules.search.infra;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexBootstrap {

    private final ElasticsearchClient esClient;
    private final ElasticsearchProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapIndex() {
        String indexName = properties.getIndex().getIndexName();
        String alias = properties.getIndex().getAlias();

        try {
            boolean indexExists = esClient.indices().exists(
                    ExistsRequest.of(e -> e.index(indexName))
            ).value();

            if (!indexExists) {
                try (InputStream mappingStream = new ClassPathResource("elasticsearch/walkthroughs-mapping.json").getInputStream();
                     JsonReader reader = Json.createReader(mappingStream)) {

                    JsonObject mappingJson = reader.readObject();

                    esClient.indices().create(c -> c
                            .index(indexName)
                            .aliases(alias, a -> a.isWriteIndex(true))
                            .withJson(new java.io.StringReader(mappingJson.toString()))
                    );

                    log.info("Created Elasticsearch index '{}' with alias '{}'", indexName, alias);
                }
            } else {
                log.info("Elasticsearch index '{}' already exists", indexName);
            }
        } catch (IOException e) {
            log.warn("Failed to bootstrap Elasticsearch index '{}': {}", indexName, e.getMessage());
        }
    }
}
