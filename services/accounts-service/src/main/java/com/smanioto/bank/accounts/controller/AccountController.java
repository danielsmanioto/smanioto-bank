package com.smanioto.bank.accounts.controller;

import com.smanioto.bank.accounts.dto.AccountResponse;
import com.smanioto.bank.accounts.dto.MovementResponse;
import com.smanioto.bank.accounts.dto.OpenAccountRequest;
import com.smanioto.bank.accounts.dto.TransferRequest;
import com.smanioto.bank.accounts.dto.TransferResponse;
import com.smanioto.bank.accounts.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        var account = accountService.openAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getBank(),
                account.getBranch(),
                account.getNumber(),
                account.getBalance()
            ));
    }

    @GetMapping("/{accountId}/statement")
    public List<MovementResponse> statement(@PathVariable UUID accountId) {
        return accountService.getStatement(accountId).stream()
            .map(movement -> new MovementResponse(
                movement.getId(),
                movement.getTransferId(),
                movement.getType(),
                movement.getAmount(),
                movement.getDescription(),
                movement.getCreatedAt()
            ))
            .toList();
    }

    @PostMapping("/transfer")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        var transferId = accountService.transfer(request);
        return new TransferResponse(transferId, request.fromAccountId(), request.toAccountId(), request.amount());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
