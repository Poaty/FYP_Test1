package com.fyp.moviecommunity.omdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OmdbMovie {

    // full movie lookup fields used by MovieService
    @JsonProperty("Title")
    private String title;

    @JsonProperty("Year")
    private String year;

    @JsonProperty("imdbID")
    private String imdbId;

    @JsonProperty("Poster")
    private String poster;

    @JsonProperty("Plot")
    private String plot;

    @JsonProperty("Genre")
    private String genre;

    @JsonProperty("Director")
    private String director;

    @JsonProperty("Response")
    private String response;

    @JsonProperty("Error")
    private String error;
}