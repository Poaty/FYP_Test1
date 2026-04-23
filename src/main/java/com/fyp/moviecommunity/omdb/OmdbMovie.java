package com.fyp.moviecommunity.omdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The full movie details returned by OMDb's ?i=imdbID lookup.
 * More fields exist (Rated, Runtime, Awards, Writer, Actors, ratings...) -- only
 * pulling what we actually persist to the movies table.
 */
@Data
@NoArgsConstructor
public class OmdbMovie {
    @JsonProperty("Title")    private String title;
    @JsonProperty("Year")     private String year;
    @JsonProperty("imdbID")   private String imdbId;
    @JsonProperty("Poster")   private String poster;    // URL or "N/A"
    @JsonProperty("Plot")     private String plot;
    @JsonProperty("Genre")    private String genre;     // comma-separated
    @JsonProperty("Director") private String director;
    @JsonProperty("Response") private String response;  // "True" if OMDb found it
    @JsonProperty("Error")    private String error;
}
