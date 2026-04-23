package com.fyp.moviecommunity.omdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One result from OMDb's search endpoint (?s=...).
 *
 * OMDb returns PascalCase keys ("Title", "Year") -- @JsonProperty maps
 * them to our camelCase fields. Poster is sometimes the string "N/A"
 * when OMDb has no image; handle that upstream.
 */
@Data
@NoArgsConstructor
public class OmdbSearchItem {
    @JsonProperty("Title")  private String title;
    @JsonProperty("Year")   private String year;    // string, not int -- OMDb sends "2014", "2014–" etc.
    @JsonProperty("imdbID") private String imdbId;
    @JsonProperty("Type")   private String type;    // "movie", "series", "episode"
    @JsonProperty("Poster") private String poster;
}
