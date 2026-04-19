package com.pet.walkthroughserver.modules.search.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.elasticsearch")
public class ElasticsearchProperties {

    private String uris = "http://localhost:9200";
    private String connectTimeout = "2s";
    private String readTimeout = "5s";
    private IndexProperties index = new IndexProperties();

    @Getter
    @Setter
    public static class IndexProperties {
        private String alias = "walkthroughs";
        private String version = "v1";

        public String getIndexName() {
            return alias + "-" + version;
        }
    }
}
