package com.fyp.moviecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Comment on an event. Identical shape to CreateCommentForm; kept separate
 *  so we don't entangle the post and event flows. */
@Getter
@Setter
@NoArgsConstructor
public class CreateEventCommentForm {

    @NotBlank(message = "Write something")
    @Size(min = 1, max = 2000, message = "Up to 2000 characters")
    private String content;
}
