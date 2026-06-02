package com.smanioto.bank.accounts.service;

import com.smanioto.bank.accounts.dto.OpenAccountRequest;
import com.smanioto.bank.accounts.dto.TransferRequest;
import com.smanioto.bank.accounts.integration.PeopleClient;
import com.smanioto.bank.accounts.model.Account;
import com.smanioto.bank.accounts.model.Movement;
import com.smanioto.bank.accounts.model.MovementType;
import com.smanioto.bank.accounts.repository.AccountRepository;
import com.smanioto.bank.accounts.repository.MovementRepository;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode MONETARY_ROUNDING = RoundingMode.HALF_EVEN;

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final PeopleClient peopleClient;

    public AccountService(AccountRepository accountRepository,
                          MovementRepository movementRepository,
                          PeopleClient peopleClient) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.peopleClient = peopleClient;
    }

    public Account openAccount(OpenAccountRequest request) {
        if (!peopleClient.customerExists(request.customerId())) {
            throw new IllegalArgumentException("Cliente PF não encontrado");
        }

        try {
            return accountRepository.save(new Account(
                UUID.randomUUID(),
                request.customerId(),
                request.bank().trim(),
                request.branch().trim(),
                request.number().trim(),
                request.initialBalance().setScale(MONETARY_SCALE, MONETARY_ROUNDING)
            ));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Conta já cadastrada com o número informado");
        }
    }

    public List<Movement> getStatement(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new IllegalArgumentException("Conta não encontrada");
        }
        return movementRepository.findByAccountIdOrderByCreatedAtAscIdAsc(accountId);
    }

    @Transactional
    public UUID transfer(TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new IllegalArgumentException("Conta de origem e destino devem ser diferentes");
        }

        var source = accountRepository.findByIdForUpdate(request.fromAccountId())
            .orElseThrow(() -> new IllegalArgumentException("Conta origem inexistente"));
        var destination = accountRepository.findByIdForUpdate(request.toAccountId())
            .orElseThrow(() -> new IllegalArgumentException("Conta destino inexistente"));

        if (!source.getBank().equals(destination.getBank())) {
            throw new IllegalArgumentException("Transferência interna exige contas do mesmo banco");
        }

        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para transferência");
        }

        source.setBalance(source.getBalance().subtract(request.amount()));
        destination.setBalance(destination.getBalance().add(request.amount()));
        accountRepository.save(source);
        accountRepository.save(destination);

        var transferId = UUID.randomUUID();
        var now = Instant.now();
        movementRepository.save(new Movement(
            UUID.randomUUID(),
            source.getId(),
            transferId,
            MovementType.DEBIT,
            request.amount(),
            "Transferência enviada para conta " + destination.getNumber(),
            now
        ));
        movementRepository.save(new Movement(
            UUID.randomUUID(),
            destination.getId(),
            transferId,
            MovementType.CREDIT,
            request.amount(),
            "Transferência recebida da conta " + source.getNumber(),
            now
        ));

        return transferId;
    }
}
