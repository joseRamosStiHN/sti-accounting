package com.sti.accounting.core;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final SecurityUserDto user;
    private final String tenantId;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(SecurityUserDto user, String tenantId, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.tenantId = tenantId;
        this.authorities = authorities;
    }



    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return user.getUserName();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }


}
