package com.sti.accounting.controllers;


import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.models.BalancesRequest;
import com.sti.accounting.services.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@RestController
@RequestMapping("/api/v1/accounts")
public class AccountingController {

    private final AccountService accountService;

    public AccountingController(AccountService accountService) {
        this.accountService = accountService;
    }


    @GetMapping()
    public ResponseEntity<Object> getAccounts() {
        List<AccountRequest> accounts = accountService.getAllAccount();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountRequest> getAccountById(@PathVariable Long id) {
        AccountRequest accountRequest;
        try {
            accountRequest = accountService.getById(id);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
        return ResponseEntity.ok(accountRequest);
    }

    @PostMapping
    public ResponseEntity<Object> createAccount(@Valid @RequestBody AccountRequest accountRequest) {
        AccountEntity newAccount = accountService.createAccount(accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(newAccount);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateAccount(@PathVariable("id") Long id, @Valid @RequestBody AccountRequest accountRequest) {
        AccountEntity updateAccount = accountService.updateAccount(id, accountRequest);
        if (updateAccount != null) {
            return ResponseEntity.ok(updateAccount);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteAccount(@PathVariable("id") Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

}
