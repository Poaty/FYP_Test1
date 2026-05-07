package com.fyp.moviecommunity.security;

import com.fyp.moviecommunity.model.User;
import com.fyp.moviecommunity.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // spring security calls this during login
        User user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account: " + username));

        return new AppUserDetails(user);
    }
}