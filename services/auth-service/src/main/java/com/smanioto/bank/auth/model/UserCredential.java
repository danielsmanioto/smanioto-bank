package com.smanioto.bank.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    protected UserCredential() {
    }

    public UserCredential(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
