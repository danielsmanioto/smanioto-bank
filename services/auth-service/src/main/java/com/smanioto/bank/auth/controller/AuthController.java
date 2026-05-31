package com.smanioto.bank.auth.controller;

import com.smanioto.bank.auth.dto.LoginRequest;
import com.smanioto.bank.auth.dto.RegisterRequest;
import com.smanioto.bank.auth.dto.TokenResponse;
import com.smanioto.bank.auth.dto.ValidationResponse;
import com.smanioto.bank.auth.security.JwtService;
import com.smanioto.bank.auth.service.UserCredentialsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserCredentialsService userCredentialsService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserCredentialsService userCredentialsService,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService) {
        this.userCredentialsService = userCredentialsService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        userCredentialsService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        var token = jwtService.generateToken((UserDetails) auth.getPrincipal());
        return new TokenResponse(token, "Bearer");
    }

    @GetMapping("/validate")
    public ValidationResponse validate(Authentication authentication) {
        return new ValidationResponse(true, authentication.getName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas");
    }
}
