package com.fyp.moviecommunity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form-backing object for signup. Kept separate from {@link com.fyp.moviecommunity.model.User}
 * so that validation rules, field order, and raw password field don't leak
 * onto the entity (the User entity never sees or stores a raw password).
 */
@Getter
@Setter
@NoArgsConstructor
public class SignupForm {

    @NotBlank(message = "Choose a username")
    @Size(min = 3, max = 30, message = "Username must be 3-30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Letters, numbers, and underscores only")
    private String username;

    @NotBlank(message = "Enter your email")
    @Email(message = "That doesn't look like a valid email")
    private String email;

    @NotBlank(message = "Choose a password")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;
}
