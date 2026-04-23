package com.fyp.moviecommunity.omdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OMDb wraps search results in an envelope:
 *   { "Search": [...], "totalResults": "123", "Response": "True" }
 *
 * On zero hits it sends { "Response": "False", "Error": "Movie not found!" }
 * -- search() returns an empty list in that case.
 */
@Data
@NoArgsConstructor
public class OmdbSearchResponse {
    @JsonProperty("Search")       private List<OmdbSearchItem> search;
    @JsonProperty("totalResults") private String totalResults;
    @JsonProperty("Response")     private String response;   // "True" / "False"
    @JsonProperty("Error")        private String error;
}
