package com.smanioto.bank.auth.service;

import com.smanioto.bank.auth.model.UserCredential;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserCredentialsService implements UserDetailsService {

    private final Map<String, UserCredential> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public UserCredentialsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String username, String rawPassword) {
        var normalized = username.trim().toLowerCase();
        var created = users.putIfAbsent(normalized, new UserCredential(normalized, passwordEncoder.encode(rawPassword)));
        if (created != null) {
            throw new IllegalArgumentException("Usuário já existe");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var normalized = username.trim().toLowerCase();
        var credential = users.get(normalized);
        if (credential == null) {
            throw new UsernameNotFoundException("Usuário não encontrado");
        }
        return User.withUsername(credential.username())
            .password(credential.passwordHash())
            .roles("USER")
            .build();
    }
}
