package com.pet.walkthroughserver.modules.search.infra;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.pet.walkthroughserver.modules.search.business.models.SearchQuery;
import com.pet.walkthroughserver.modules.search.business.models.SearchResult;
import com.pet.walkthroughserver.modules.search.business.models.WalkthroughDocument;
import com.pet.walkthroughserver.modules.search.exceptions.IndexingException;
import com.pet.walkthroughserver.modules.search.exceptions.SearchException;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.InnerHitsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WalkthroughEsRepository {

    private final ElasticsearchClient esClient;
    private final ElasticsearchProperties properties;

    public void index(WalkthroughDocument document) {
        try {
            String indexName = properties.getIndex().getAlias();
            esClient.index(IndexRequest.of(i -> i
                    .index(indexName)
                    .id(document.getId().toString())
                    .document(document)
            ));
        } catch (IOException e) {
            throw new IndexingException("Failed to index walkthrough " + document.getId(), e);
        }
    }

    public void delete(UUID walkthroughId) {
        try {
            String indexName = properties.getIndex().getAlias();
            esClient.delete(DeleteRequest.of(d -> d
                    .index(indexName)
                    .id(walkthroughId.toString())
            ));
        } catch (IOException e) {
            throw new IndexingException("Failed to delete walkthrough " + walkthroughId, e);
        }
    }

    public SearchResult search(SearchQuery query) {
        try {
            String indexName = properties.getIndex().getAlias();
            SearchRequest searchRequest = buildSearchRequest(indexName, query);
            SearchResponse<WalkthroughDocument> response = esClient.search(searchRequest, WalkthroughDocument.class);
            return mapResponse(response, query);
        } catch (IOException e) {
            throw new SearchException("Failed to execute search", e);
        }
    }

    private SearchRequest buildSearchRequest(String indexName, SearchQuery query) {
        return SearchRequest.of(s -> {
            s.index(indexName)
                    .from(query.getPage() * query.getSize())
                    .size(query.getSize());

            // Build bool query
            s.query(buildBoolQuery(query));

            // Sorting
            if ("relevance".equals(query.getSort()) || query.getSort() == null) {
                s.sort(sort -> sort.score(sc -> sc.order(SortOrder.Desc)));
            } else if ("createdAt,desc".equals(query.getSort())) {
                s.sort(sort -> sort.field(f -> f.field("createdAt").order(SortOrder.Desc)));
            } else if ("createdAt,asc".equals(query.getSort())) {
                s.sort(sort -> sort.field(f -> f.field("createdAt").order(SortOrder.Asc)));
            }

            // Highlighting
            s.highlight(h -> h
                    .preTags("<mark>")
                    .postTags("</mark>")
                    .fields("title", hf -> hf)
                    .fields("description", hf -> hf)
            );

            // Aggregations (facets)
            s.aggregations("by_status", a -> a.terms(t -> t.field("status").size(10)));
            s.aggregations("by_repo", a -> a.terms(t -> t.field("repoFull").size(20)));
            s.aggregations("by_author", a -> a.terms(t -> t.field("authorName").size(20)));

            return s;
        });
    }

    private Query buildBoolQuery(SearchQuery query) {
        return Query.of(q -> q.bool(b -> {
            // Must: full-text search
            if (query.getQuery() != null && !query.getQuery().isBlank()) {
                b.must(m -> m.bool(textQuery -> {
                    // Main fields multi_match
                    textQuery.should(sh -> sh.multiMatch(mm -> mm
                            .query(query.getQuery())
                            .fields("title^3", "description")
                            .fuzziness("AUTO")
                            .prefixLength(1)
                    ));

                    // Nested query for chapters
                    textQuery.should(sh -> sh.nested(n -> n
                            .path("chapters")
                            .query(nq -> nq.multiMatch(mm -> mm
                                    .query(query.getQuery())
                                    .fields("chapters.title^2", "chapters.content")
                                    .fuzziness("AUTO")
                                    .prefixLength(1)
                            ))
                            .innerHits(ih -> ih
                                    .highlight(h -> h
                                            .preTags("<mark>")
                                            .postTags("</mark>")
                                            .fields("chapters.title", hf -> hf)
                                            .fields("chapters.content", hf -> hf)
                                    )
                            )
                    ));

                    textQuery.minimumShouldMatch("1");
                    return textQuery;
                }));
            }

            // Filters
            if (query.getRepository() != null && !query.getRepository().isBlank()) {
                b.filter(f -> f.term(t -> t.field("repoFull").value(query.getRepository())));
            }

            if (query.getAuthorIds() != null && !query.getAuthorIds().isEmpty()) {
                List<FieldValue> values = query.getAuthorIds().stream()
                        .map(id -> FieldValue.of(id.toString()))
                        .toList();
                b.filter(f -> f.terms(t -> t.field("userId").terms(tv -> tv.value(values))));
            }

            if (query.getStatuses() != null && !query.getStatuses().isEmpty()) {
                List<FieldValue> values = query.getStatuses().stream()
                        .map(FieldValue::of)
                        .toList();
                b.filter(f -> f.terms(t -> t.field("status").terms(tv -> tv.value(values))));
            }

            if (query.getCreatedFrom() != null || query.getCreatedTo() != null) {
                b.filter(f -> f.range(r -> r.date(d -> {
                    if (query.getCreatedFrom() != null) {
                        d.gte(query.getCreatedFrom().toString());
                    }
                    if (query.getCreatedTo() != null) {
                        d.lte(query.getCreatedTo().toString());
                    }
                    return d.field("createdAt");
                })));
            }

            return b;
        }));
    }

    private SearchResult mapResponse(SearchResponse<WalkthroughDocument> response, SearchQuery query) {
        List<SearchResult.SearchHit> hits = new ArrayList<>();

        for (Hit<WalkthroughDocument> hit : response.hits().hits()) {
            Map<String, List<String>> highlights = new HashMap<>();
            List<SearchResult.ChapterHit> chapterHits = new ArrayList<>();

            // Map field highlights
            if (hit.highlight() != null) {
                if (hit.highlight().containsKey("title")) {
                    highlights.put("title", hit.highlight().get("title"));
                }
                if (hit.highlight().containsKey("description")) {
                    highlights.put("description", hit.highlight().get("description"));
                }
            }

            // Map chapter inner hits
            if (hit.innerHits() != null && hit.innerHits().containsKey("chapters")) {
                InnerHitsResult innerHitsResult = hit.innerHits().get("chapters");
                var innerHits = innerHitsResult.hits().hits();
                for (var innerHit : innerHits) {
                    int offset = innerHit.nested() != null ? innerHit.nested().offset() : 0;
                    if (innerHit.highlight() != null) {
                        for (Map.Entry<String, List<String>> entry : innerHit.highlight().entrySet()) {
                            for (String snippet : entry.getValue()) {
                                chapterHits.add(SearchResult.ChapterHit.builder()
                                        .chapterIndex(offset)
                                        .field(entry.getKey().replace("chapters.", ""))
                                        .snippet(snippet)
                                        .build());
                            }
                        }
                    }
                }
            }

            hits.add(SearchResult.SearchHit.builder()
                    .document(hit.source())
                    .score(hit.score() != null ? hit.score() : 0)
                    .highlights(highlights)
                    .chapterHits(chapterHits)
                    .build());
        }

        // Map facets/aggregations
        Map<String, List<SearchResult.FacetEntry>> facets = new HashMap<>();
        if (response.aggregations() != null) {
            mapFacet(response, "by_status", "status", facets);
            mapFacet(response, "by_repo", "repository", facets);
            mapFacet(response, "by_author", "authors", facets);
        }

        long total = response.hits().total() != null ? response.hits().total().value() : 0;

        return SearchResult.builder()
                .total(total)
                .page(query.getPage())
                .size(query.getSize())
                .hits(hits)
                .facets(facets)
                .build();
    }

    private void mapFacet(SearchResponse<WalkthroughDocument> response, String aggName, String facetKey,
                          Map<String, List<SearchResult.FacetEntry>> facets) {
        if (response.aggregations().containsKey(aggName)) {
            var agg = response.aggregations().get(aggName);
            if (agg.isSterms()) {
                List<SearchResult.FacetEntry> entries = agg.sterms().buckets().array().stream()
                        .map(bucket -> SearchResult.FacetEntry.builder()
                                .value(bucket.key().stringValue())
                                .count(bucket.docCount())
                                .build())
                        .toList();
                facets.put(facetKey, entries);
            }
        }
    }
}
