package com.sti.accounting.controllers;


import com.sti.accounting.models.AccountCategory;
import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.models.AccountResponse;
import com.sti.accounting.services.AccountService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/accounts")

public class AccountingController {


    private final AccountService accountService;


    public AccountingController(AccountService accountService) {
        this.accountService = accountService;

    }

    @GetMapping()
    public List<AccountResponse> getAccounts() {
        return accountService.getAllAccount();
    }

    @GetMapping("/{id}")
    public AccountResponse GetAccountById(@PathVariable Long id) {
        return accountService.getById(id);
    }

    @PostMapping
    public AccountResponse CreateAccount(@Validated @RequestBody AccountRequest accountRequest) {
        return accountService.createAccount(accountRequest);
    }

    @PutMapping("/{id}")
    public AccountResponse updateAccount(@PathVariable("id") Long id, @Validated @RequestBody AccountRequest accountRequest) {
        return accountService.updateAccount(id, accountRequest);
    }

    @GetMapping("/categories")
    public List<AccountCategory> GetAllCategories() {
        return accountService.getAllCategories();
    }

}
