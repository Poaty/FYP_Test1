package com.fyp.moviecommunity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who's allowed where.
 *
 * Public: /, /signup, /login, static assets. Everything else needs a login.
 * Login form is our own Thymeleaf page at /login (not Spring's default one).
 * Logout POSTs to /logout and bounces to /?logout.
 * CSRF stays on -- Thymeleaf's th:action drops the token in for us.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/signup", "/privacy", "/terms", "/guidelines",
                                 "/css/**", "/js/**", "/images/**", "/webjars/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/feed", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/?logout")
                .permitAll()
            );
        return http.build();
    }

    // BCrypt with cost 10. Default, fine for us. Bump to 12+ if we ever care about
    // hardening against offline cracking.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
