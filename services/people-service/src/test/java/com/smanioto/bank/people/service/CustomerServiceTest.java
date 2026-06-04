package com.smanioto.bank.people.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.smanioto.bank.people.dto.CreateCustomerRequest;
import com.smanioto.bank.people.model.Customer;
import com.smanioto.bank.people.repository.CustomerRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void create_shouldSaveNormalizedCustomer() {
        var request = new CreateCustomerRequest("  João da Silva  ", "39053344705", "  JOAO@EMAIL.COM  ");
        var saved = new Customer(UUID.randomUUID(), "João da Silva", "39053344705", "joao@email.com");
        given(customerRepository.save(any())).willReturn(saved);

        var result = customerService.create(request);

        assertThat(result).isEqualTo(saved);
        verify(customerRepository).save(argThat(c ->
            "João da Silva".equals(c.getFullName()) && "joao@email.com".equals(c.getEmail())
        ));
    }

    @Test
    void create_shouldTrimFullNameAndLowercaseEmail() {
        var request = new CreateCustomerRequest("  Maria Silva  ", "39053344705", "  MARIA@BANCO.COM  ");
        given(customerRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var result = customerService.create(request);

        assertThat(result.getFullName()).isEqualTo("Maria Silva");
        assertThat(result.getEmail()).isEqualTo("maria@banco.com");
    }

    @Test
    void create_whenDuplicateCpfOrEmail_shouldThrowException() {
        var request = new CreateCustomerRequest("João", "39053344705", "joao@email.com");
        given(customerRepository.save(any())).willThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> customerService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cliente já cadastrado com CPF ou e-mail informado");
    }

    @Test
    void exists_whenCustomerPresent_shouldReturnTrue() {
        var id = UUID.randomUUID();
        given(customerRepository.existsById(id)).willReturn(true);

        assertThat(customerService.exists(id)).isTrue();
    }

    @Test
    void exists_whenCustomerAbsent_shouldReturnFalse() {
        var id = UUID.randomUUID();
        given(customerRepository.existsById(id)).willReturn(false);

        assertThat(customerService.exists(id)).isFalse();
    }
}
