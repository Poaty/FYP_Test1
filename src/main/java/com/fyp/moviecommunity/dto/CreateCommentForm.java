package com.fyp.moviecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCommentForm {

    // post comment text
    @NotBlank(message = "Write something")
    @Size(min = 1, max = 2000, message = "Comments are capped at 2000 characters")
    private String content;
}