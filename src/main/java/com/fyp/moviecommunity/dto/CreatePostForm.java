package com.fyp.moviecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What the user submits when they hit "Post it" on the write page.
 *
 * imdbId is carried through as a hidden field from the previous page
 * (they picked the movie there; we don't want them picking it again).
 */
@Getter
@Setter
@NoArgsConstructor
public class CreatePostForm {

    @NotBlank(message = "Something went wrong -- pick a movie again")
    private String imdbId;

    @NotBlank(message = "Write something, anything")
    @Size(min = 1, max = 5000, message = "Up to 5000 characters")
    private String content;
}
