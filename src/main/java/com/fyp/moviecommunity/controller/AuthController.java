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
        // spring Security handles the actual login POST
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        // avoid replacing the form if validation sent the user back here
        if (!model.containsAttribute("signupForm")) {
            model.addAttribute("signupForm", new SignupForm());
        }
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signupForm") SignupForm form,
                         BindingResult result) {

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

        // only the hash is stored
        u.setPasswordHash(encoder.encode(form.getPassword()));
        users.save(u);

        // the login template uses this flag to show the success message
        return "redirect:/login?registered";
    }
}