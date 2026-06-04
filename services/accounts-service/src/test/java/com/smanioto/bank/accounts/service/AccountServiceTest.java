package com.smanioto.bank.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.smanioto.bank.accounts.dto.OpenAccountRequest;
import com.smanioto.bank.accounts.dto.TransferRequest;
import com.smanioto.bank.accounts.integration.PeopleClient;
import com.smanioto.bank.accounts.model.Account;
import com.smanioto.bank.accounts.model.Movement;
import com.smanioto.bank.accounts.model.MovementType;
import com.smanioto.bank.accounts.repository.AccountRepository;
import com.smanioto.bank.accounts.repository.MovementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private PeopleClient peopleClient;

    @InjectMocks
    private AccountService accountService;

    private UUID customerId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @Test
    void openAccount_whenCustomerExists_shouldSaveAndReturnAccount() {
        var request = new OpenAccountRequest(customerId, "341", "0001", "12345-6", new BigDecimal("100.00"));
        var expected = new Account(UUID.randomUUID(), customerId, "341", "0001", "12345-6", new BigDecimal("100.00"));
        given(peopleClient.customerExists(customerId)).willReturn(true);
        given(accountRepository.save(any())).willReturn(expected);

        var result = accountService.openAccount(request);

        assertThat(result).isEqualTo(expected);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void openAccount_whenCustomerNotFound_shouldThrowException() {
        var request = new OpenAccountRequest(customerId, "341", "0001", "12345-6", BigDecimal.ZERO);
        given(peopleClient.customerExists(customerId)).willReturn(false);

        assertThatThrownBy(() -> accountService.openAccount(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cliente PF não encontrado");

        verify(accountRepository, times(0)).save(any());
    }

    @Test
    void openAccount_whenDuplicateAccountNumber_shouldThrowException() {
        var request = new OpenAccountRequest(customerId, "341", "0001", "12345-6", BigDecimal.ZERO);
        given(peopleClient.customerExists(customerId)).willReturn(true);
        given(accountRepository.save(any())).willThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> accountService.openAccount(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Conta já cadastrada com o número informado");
    }

    @Test
    void openAccount_shouldNormalizeWhitespaceInFields() {
        var request = new OpenAccountRequest(customerId, " 341 ", " 0001 ", " 12345-6 ", new BigDecimal("50.00"));
        given(peopleClient.customerExists(customerId)).willReturn(true);
        given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var result = accountService.openAccount(request);

        assertThat(result.getBank()).isEqualTo("341");
        assertThat(result.getBranch()).isEqualTo("0001");
        assertThat(result.getNumber()).isEqualTo("12345-6");
    }

    @Test
    void getAccount_whenFound_shouldReturnAccount() {
        var account = new Account(accountId, customerId, "341", "0001", "12345-6", BigDecimal.TEN);
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

        var result = accountService.getAccount(accountId);

        assertThat(result).isEqualTo(account);
    }

    @Test
    void getAccount_whenNotFound_shouldThrowException() {
        given(accountRepository.findById(accountId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(accountId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Conta não encontrada");
    }

    @Test
    void getStatement_whenAccountExists_shouldReturnMovementsInOrder() {
        var mov1 = new Movement(UUID.randomUUID(), accountId, UUID.randomUUID(),
            MovementType.CREDIT, new BigDecimal("200.00"), "Crédito inicial", Instant.now());
        var mov2 = new Movement(UUID.randomUUID(), accountId, UUID.randomUUID(),
            MovementType.DEBIT, new BigDecimal("50.00"), "Débito", Instant.now().plusSeconds(1));
        given(accountRepository.existsById(accountId)).willReturn(true);
        given(movementRepository.findByAccountIdOrderByCreatedAtAscIdAsc(accountId)).willReturn(List.of(mov1, mov2));

        var result = accountService.getStatement(accountId);

        assertThat(result).hasSize(2).containsExactly(mov1, mov2);
    }

    @Test
    void getStatement_whenAccountNotFound_shouldThrowException() {
        given(accountRepository.existsById(accountId)).willReturn(false);

        assertThatThrownBy(() -> accountService.getStatement(accountId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Conta não encontrada");

        verify(movementRepository, times(0)).findByAccountIdOrderByCreatedAtAscIdAsc(any());
    }

    @Test
    void transfer_shouldDebitSourceAndCreditDestination() {
        var sourceId = UUID.randomUUID();
        var destId = UUID.randomUUID();
        var source = new Account(sourceId, customerId, "341", "0001", "11111-1", new BigDecimal("500.00"));
        var dest = new Account(destId, customerId, "341", "0001", "22222-2", new BigDecimal("50.00"));
        var request = new TransferRequest(sourceId, destId, new BigDecimal("100.00"));

        given(accountRepository.findByIdForUpdate(sourceId)).willReturn(Optional.of(source));
        given(accountRepository.findByIdForUpdate(destId)).willReturn(Optional.of(dest));

        var transferId = accountService.transfer(request);

        assertThat(source.getBalance()).isEqualByComparingTo("400.00");
        assertThat(dest.getBalance()).isEqualByComparingTo("150.00");
        assertThat(transferId).isNotNull();
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(movementRepository, times(2)).save(any(Movement.class));
    }

    @Test
    void transfer_whenSameAccount_shouldThrowException() {
        var request = new TransferRequest(accountId, accountId, BigDecimal.TEN);

        assertThatThrownBy(() -> accountService.transfer(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Conta de origem e destino devem ser diferentes");

        verify(accountRepository, times(0)).findByIdForUpdate(any());
    }

    @Test
    void transfer_whenSourceNotFound_shouldThrowException() {
        var destId = UUID.randomUUID();
        given(accountRepository.findByIdForUpdate(accountId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transfer(new TransferRequest(accountId, destId, BigDecimal.TEN)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Conta origem inexistente");
    }

    @Test
    void transfer_whenDestinationNotFound_shouldThrowException() {
        var destId = UUID.randomUUID();
        var source = new Account(accountId, customerId, "341", "0001", "11111-1", new BigDecimal("500.00"));
        given(accountRepository.findByIdForUpdate(accountId)).willReturn(Optional.of(source));
        given(accountRepository.findByIdForUpdate(destId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transfer(new TransferRequest(accountId, destId, BigDecimal.TEN)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Conta destino inexistente");
    }

    @Test
    void transfer_whenInsufficientBalance_shouldThrowException() {
        var sourceId = UUID.randomUUID();
        var destId = UUID.randomUUID();
        var source = new Account(sourceId, customerId, "341", "0001", "11111-1", new BigDecimal("10.00"));
        var dest = new Account(destId, customerId, "341", "0001", "22222-2", BigDecimal.ZERO);
        given(accountRepository.findByIdForUpdate(sourceId)).willReturn(Optional.of(source));
        given(accountRepository.findByIdForUpdate(destId)).willReturn(Optional.of(dest));

        assertThatThrownBy(() -> accountService.transfer(new TransferRequest(sourceId, destId, new BigDecimal("50.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Saldo insuficiente para transferência");

        verify(accountRepository, times(0)).save(any());
    }

    @Test
    void transfer_whenDifferentBanks_shouldThrowException() {
        var sourceId = UUID.randomUUID();
        var destId = UUID.randomUUID();
        var source = new Account(sourceId, customerId, "341", "0001", "11111-1", new BigDecimal("500.00"));
        var dest = new Account(destId, customerId, "237", "0001", "22222-2", BigDecimal.ZERO);
        given(accountRepository.findByIdForUpdate(sourceId)).willReturn(Optional.of(source));
        given(accountRepository.findByIdForUpdate(destId)).willReturn(Optional.of(dest));

        assertThatThrownBy(() -> accountService.transfer(new TransferRequest(sourceId, destId, new BigDecimal("100.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transferência interna exige contas do mesmo banco");
    }
}
