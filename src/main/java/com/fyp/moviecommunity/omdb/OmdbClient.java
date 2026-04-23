package com.fyp.moviecommunity.omdb;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin wrapper around OMDb HTTP endpoints.
 *
 *   search(q)          -> GET /?s=q&apikey=X&type=movie
 *   getByImdbId(id)    -> GET /?i=id&apikey=X
 *
 * Returns empty on anything non-success. We don't bubble OMDb errors up --
 * the UI just shows "no results" which is the right UX for this MVP.
 * If we ever need better error handling (rate limits, network flakes),
 * wrap this in a Resilience4j retry or something.
 */
@Component
public class OmdbClient {

    private static final Logger log = LoggerFactory.getLogger(OmdbClient.class);

    private final RestClient http;
    private final String apiKey;

    public OmdbClient(@Value("${omdb.api.base-url}") String baseUrl,
                      @Value("${omdb.api.key}") String apiKey) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    /** Search by (partial) title. Empty list on error or no hits. */
    public List<OmdbSearchItem> search(String query) {
        if (query == null || query.isBlank()) return List.of();
        try {
            OmdbSearchResponse resp = http.get()
                    .uri(uri -> uri.queryParam("s", query)
                                   .queryParam("apikey", apiKey)
                                   .queryParam("type", "movie")
                                   .build())
                    .retrieve()
                    .body(OmdbSearchResponse.class);

            if (resp == null || !"True".equals(resp.getResponse()) || resp.getSearch() == null) {
                return List.of();
            }
            return resp.getSearch();
        } catch (RestClientException e) {
            log.warn("OMDb search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    /** Full movie details by IMDb ID, e.g. "tt0816692". */
    public Optional<OmdbMovie> getByImdbId(String imdbId) {
        if (imdbId == null || imdbId.isBlank()) return Optional.empty();
        try {
            OmdbMovie m = http.get()
                    .uri(uri -> uri.queryParam("i", imdbId)
                                   .queryParam("apikey", apiKey)
                                   .build())
                    .retrieve()
                    .body(OmdbMovie.class);

            if (m == null || !"True".equals(m.getResponse())) {
                return Optional.empty();
            }
            return Optional.of(m);
        } catch (RestClientException e) {
            log.warn("OMDb lookup failed for '{}': {}", imdbId, e.getMessage());
            return Optional.empty();
        }
    }
}
