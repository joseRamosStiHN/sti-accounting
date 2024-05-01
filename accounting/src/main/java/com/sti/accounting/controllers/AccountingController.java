package com.sti.accounting.controllers;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.models.Constant;
import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.services.AccountService;
import com.sti.accounting.utils.Util;
import jakarta.ws.rs.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
    public List<AccountRequest> getAccounts() {
        return accountService.GetAllAccount();
    }

    @GetMapping("/{id}")
    public AccountRequest getAccountById(@PathVariable Long id) {
        return accountService.GetById(id);
    }
    //TODO: NO SE DEBE RETORNAR EL ENTITY
    @PostMapping
    public ResponseEntity<Object> createAccount(@Validated @RequestBody AccountRequest accountRequest) {
        try {
            AccountEntity newAccount = accountService.CreateAccount(accountRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(this.util.setSuccessResponse(newAccount, HttpStatus.CREATED));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error creating account"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }
    //TODO: NO SE DEBE RETORNAR EL ENTITY
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateAccount(@PathVariable("id") Long id, @Validated @RequestBody AccountRequest accountRequest) {

        try {
            AccountEntity updateAccount = accountService.UpdateAccount(id, accountRequest);
            return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(updateAccount, HttpStatus.OK));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error updating account"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }


}
