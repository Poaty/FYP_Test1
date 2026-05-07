package com.fyp.moviecommunity.omdb;

import com.fyp.moviecommunity.service.PerformanceMetrics;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OmdbClient {

    private static final Logger log = LoggerFactory.getLogger(OmdbClient.class);

    private final RestClient http;
    private final String apiKey;
    private final PerformanceMetrics perf;

    public OmdbClient(@Value("${omdb.api.base-url}") String baseUrl,
                      @Value("${omdb.api.key}") String apiKey,
                      PerformanceMetrics perf) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.perf = perf;
    }

    public OmdbSearchResponse search(String query, int page) {
        if (query == null || query.isBlank()) return emptySearchResponse();

        // keep the page number inside omdb's limit
        int safePage = Math.max(1, Math.min(page, 100));
        long start = System.nanoTime();

        try {
            OmdbSearchResponse resp = http.get()
                    .uri(uri -> uri.queryParam("s", query)
                            .queryParam("apikey", apiKey)
                            .queryParam("type", "movie")
                            .queryParam("page", safePage)
                            .build())
                    .retrieve()
                    .body(OmdbSearchResponse.class);

            if (resp == null || !"True".equals(resp.getResponse()) || resp.getSearch() == null) {
                return emptySearchResponse();
            }

            return resp;
        } catch (RestClientException e) {
            log.warn("OMDb search failed for '{}' (page {}): {}", query, safePage, e.getMessage());
            return emptySearchResponse();
        } finally {
            perf.recordOmdbCall(System.nanoTime() - start);
        }
    }

    public List<OmdbSearchItem> search(String query) {
        OmdbSearchResponse resp = search(query, 1);
        return resp.getSearch() == null ? List.of() : resp.getSearch();
    }

    public Optional<OmdbMovie> getByImdbId(String imdbId) {
        if (imdbId == null || imdbId.isBlank()) return Optional.empty();

        long start = System.nanoTime();

        try {
            OmdbMovie movie = http.get()
                    .uri(uri -> uri.queryParam("i", imdbId)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(OmdbMovie.class);

            if (movie == null || !"True".equals(movie.getResponse())) {
                return Optional.empty();
            }

            return Optional.of(movie);
        } catch (RestClientException e) {
            log.warn("OMDb lookup failed for '{}': {}", imdbId, e.getMessage());
            return Optional.empty();
        } finally {
            perf.recordOmdbCall(System.nanoTime() - start);
        }
    }

    private static OmdbSearchResponse emptySearchResponse() {
        // used when omdb has no result or the request fails
        OmdbSearchResponse empty = new OmdbSearchResponse();
        empty.setSearch(List.of());
        empty.setTotalResults("0");
        empty.setResponse("False");
        return empty;
    }
}