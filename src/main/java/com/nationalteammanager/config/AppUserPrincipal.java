package com.nationalteammanager.config;

import com.nationalteammanager.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Prijavljeni korisnik; kontroleri iz njega čitaju ID kojim servisi filtriraju podatke. */
public class AppUserPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String fullName;
    private final String username;
    private final String passwordHash;
    private final String preferredLanguage;
    private final boolean admin;

    public AppUserPrincipal(User user) {
        this.admin = user.isAdmin();
        this.preferredLanguage = user.getPreferredLanguage();
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
    }

    public Long getId() {
        return id;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return admin
                ? List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
