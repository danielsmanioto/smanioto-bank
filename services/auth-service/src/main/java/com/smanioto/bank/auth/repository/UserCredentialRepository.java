package com.smanioto.bank.auth.repository;

import com.smanioto.bank.auth.model.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredential, String> {
}
