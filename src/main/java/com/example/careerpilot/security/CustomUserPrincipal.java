package com.example.careerpilot.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class CustomUserPrincipal
        implements UserDetails {

    private Long id;

    private String email;

    private String password;


    @Override
    public Collection<? extends GrantedAuthority>
    getAuthorities() {

        return Collections.emptyList();
    }


    @Override
    public String getUsername() {

        return email;
    }


    @Override
    public String getPassword() {

        return password;
    }
}