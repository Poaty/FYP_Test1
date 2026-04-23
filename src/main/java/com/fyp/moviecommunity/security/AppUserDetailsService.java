package com.fyp.moviecommunity.security;

import com.fyp.moviecommunity.model.User;
import com.fyp.moviecommunity.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Looks up a user at login time.
 *
 * Having this bean tells Spring Boot: "stop using your default in-memory user,
 * I've got my own." (Goodbye, generated UUID password in the console.)
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account: " + username));
        return new AppUserDetails(u);
    }
}
