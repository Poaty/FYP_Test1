package com.fyp.moviecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Form-backing object for creating a watch party.
 *
 * scheduledFor uses LocalDateTime because the HTML5 datetime-local input
 * doesn't include a timezone -- we attach the user's zone (Europe/London)
 * in the controller before persisting.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateEventForm {

    @NotBlank(message = "Pick a movie first")
    private String imdbId;

    @NotBlank(message = "Give your watch party a title")
    @Size(min = 3, max = 120, message = "Between 3 and 120 characters")
    private String title;

    @Size(max = 2000, message = "Up to 2000 characters")
    private String description;

    @NotNull(message = "Pick a date and time")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledFor;
}
