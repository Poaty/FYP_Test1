package com.fyp.moviecommunity.security;

import com.fyp.moviecommunity.model.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Makes our User entity look like a Spring Security UserDetails.
 *
 * Wrapping rather than making User implement UserDetails directly --
 * keeps the entity clean of framework stuff. If we ever swap auth
 * libraries, only this class changes.
 *
 * Everyone's a ROLE_USER for now. Add admin/moderator later if we need them.
 */
public class AppUserDetails implements UserDetails {

    private final User user;

    public AppUserDetails(User user) {
        this.user = user;
    }

    // Controllers sometimes need the full User, not just the username.
    public User getUser() { return user; }
    public Long getId()    { return user.getId(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.isAdmin()) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getUsername(); }

    // None of these matter for the MVP -- always return true.
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
