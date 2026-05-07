package com.fyp.moviecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePostForm {

    // movie picked before the write page
    @NotBlank(message = "Something went wrong -- pick a movie again")
    private String imdbId;

    @NotBlank(message = "Write something, anything")
    @Size(min = 1, max = 5000, message = "Up to 5000 characters")
    private String content;
}