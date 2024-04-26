package com.sti.accounting.controllers;


import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.Constant;
import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.services.AccountService;
import com.sti.accounting.utils.Util;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/accounts")
public class AccountingController {

    private final AccountService accountService;
    private final Util util;

    public AccountingController(AccountService accountService) {
        this.accountService = accountService;
        this.util = new Util();
    }


    @GetMapping()
    public ResponseEntity<Object> getAccounts() {
        List<AccountRequest> accounts = accountService.getAllAccount();
        return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(accounts, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getAccountById(@PathVariable Long id) {
        try {
            AccountRequest accountRequest = accountService.getById(id);
            return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(accountRequest, HttpStatus.OK));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error get Account by Id"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<Object> createAccount(@Valid @RequestBody AccountRequest accountRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setValidationError(bindingResult));
        }
        try {
            AccountEntity newAccount = accountService.createAccount(accountRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(this.util.setSuccessResponse(newAccount, HttpStatus.CREATED));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error creating account"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateAccount(@PathVariable("id") Long id, @Valid @RequestBody AccountRequest accountRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setValidationError(bindingResult));
        }
        try {
            AccountEntity updateAccount = accountService.updateAccount(id, accountRequest);
            return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(updateAccount, HttpStatus.OK));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error updating account"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }


}
