package com.fyp.moviecommunity.omdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OmdbSearchResponse {

    // wrapper around the omdb search results
    @JsonProperty("Search")
    private List<OmdbSearchItem> search;

    @JsonProperty("totalResults")
    private String totalResults;

    @JsonProperty("Response")
    private String response;

    @JsonProperty("Error")
    private String error;
}