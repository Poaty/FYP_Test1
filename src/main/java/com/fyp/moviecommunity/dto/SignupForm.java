package com.fyp.moviecommunity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignupForm {

    // shown as the user's public name
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