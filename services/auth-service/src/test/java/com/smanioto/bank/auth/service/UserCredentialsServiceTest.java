package com.smanioto.bank.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.smanioto.bank.auth.model.UserCredential;
import com.smanioto.bank.auth.repository.UserCredentialRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserCredentialsServiceTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCredentialsService userCredentialsService;

    @Test
    void register_shouldNormalizeUsernameAndEncodePassword() {
        given(userCredentialRepository.existsById("alice")).willReturn(false);
        given(passwordEncoder.encode("Senha123")).willReturn("hash-encoded");

        userCredentialsService.register("  ALICE  ", "Senha123");

        verify(userCredentialRepository).save(argThat(c ->
            "alice".equals(c.getUsername()) && "hash-encoded".equals(c.getPasswordHash())
        ));
    }

    @Test
    void register_whenUserAlreadyExists_shouldThrowException() {
        given(userCredentialRepository.existsById("alice")).willReturn(true);

        assertThatThrownBy(() -> userCredentialsService.register("alice", "senha"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Usuário já existe");

        verify(userCredentialRepository, org.mockito.Mockito.times(0)).save(any());
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWithNormalizedUsername() {
        var credential = new UserCredential("bob", "$2a$hash");
        given(userCredentialRepository.findById("bob")).willReturn(Optional.of(credential));

        var userDetails = userCredentialsService.loadUserByUsername("  BOB  ");

        assertThat(userDetails.getUsername()).isEqualTo("bob");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$hash");
        assertThat(userDetails.getAuthorities()).isNotEmpty();
    }

    @Test
    void loadUserByUsername_whenNotFound_shouldThrowException() {
        given(userCredentialRepository.findById("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userCredentialsService.loadUserByUsername("unknown"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("Usuário não encontrado");
    }
}
