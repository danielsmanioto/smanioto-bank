package com.smanioto.bank.auth.service;

import com.smanioto.bank.auth.model.UserCredential;
import com.smanioto.bank.auth.repository.UserCredentialRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCredentialsService implements UserDetailsService {

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public UserCredentialsService(UserCredentialRepository userCredentialRepository, PasswordEncoder passwordEncoder) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(String username, String rawPassword) {
        var normalized = username.trim().toLowerCase();
        if (userCredentialRepository.existsById(normalized)) {
            throw new IllegalArgumentException("Usuário já existe");
        }
        userCredentialRepository.save(new UserCredential(normalized, passwordEncoder.encode(rawPassword)));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var normalized = username.trim().toLowerCase();
        var credential = userCredentialRepository.findById(normalized)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(credential.getUsername())
            .password(credential.getPasswordHash())
            .roles("USER")
            .build();
    }
}
