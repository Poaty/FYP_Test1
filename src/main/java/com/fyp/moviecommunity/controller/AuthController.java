package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.dto.SignupForm;
import com.fyp.moviecommunity.model.User;
import com.fyp.moviecommunity.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Signup and login pages.
 *
 *   GET  /login   -- show our Thymeleaf login form
 *   GET  /signup  -- empty signup form
 *   POST /signup  -- validate, save the user, send them to the login page
 *
 * POST /login isn't here -- Spring Security's filter chain handles that.
 */
@Controller
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        if (!model.containsAttribute("signupForm")) {
            model.addAttribute("signupForm", new SignupForm());
        }
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signupForm") SignupForm form,
                         BindingResult result) {

        // Duplicate checks -- treat them like form errors so the user sees
        // a red message under the offending field, not a crash page.
        if (users.existsByUsername(form.getUsername())) {
            result.rejectValue("username", "duplicate", "That username is taken");
        }
        if (users.existsByEmail(form.getEmail())) {
            result.rejectValue("email", "duplicate", "That email already has an account");
        }
        if (result.hasErrors()) {
            return "auth/signup";
        }

        User u = new User();
        u.setUsername(form.getUsername());
        u.setEmail(form.getEmail());
        u.setPasswordHash(encoder.encode(form.getPassword()));
        users.save(u);

        return "redirect:/login?registered";
    }
}
