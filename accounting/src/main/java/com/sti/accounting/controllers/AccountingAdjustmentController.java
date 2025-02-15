package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountingAdjustmentRequest;
import com.sti.accounting.models.AccountingAdjustmentResponse;
import com.sti.accounting.services.AccountingAdjustmentService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/adjustment")
public class AccountingAdjustmentController {

    private final AccountingAdjustmentService accountingAdjustmentService;

    public AccountingAdjustmentController(AccountingAdjustmentService accountingAdjustmentService) {
        this.accountingAdjustmentService = accountingAdjustmentService;
    }

    @GetMapping
    public List<AccountingAdjustmentResponse> getAllAccountingAdjustments() {
        return accountingAdjustmentService.getAllAccountingAdjustments();
    }

    @GetMapping("/{id}")
    public AccountingAdjustmentResponse getAccountingAdjustmentsById(@PathVariable Long id) {
        return accountingAdjustmentService.getAccountingAdjustmentsById(id);
    }

    @GetMapping("/by-transaction/{transactionId}")
    public List<AccountingAdjustmentResponse> getAccountingAdjustmentsByTransactionId(@PathVariable Long transactionId) {
        return accountingAdjustmentService.getAccountingAdjustmentsByTransactionId(transactionId);
    }

    @PostMapping
    public AccountingAdjustmentResponse createAdjustment(@Validated @RequestBody AccountingAdjustmentRequest accountingAdjustmentRequest) {
        return accountingAdjustmentService.createAdjustment(accountingAdjustmentRequest);
    }

    @PutMapping("/confirm-adjustments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeAccountingAdjustmentStatus(@RequestBody List<Long> adjustmentId) {
        accountingAdjustmentService.changeAdjustmentStatus(adjustmentId);
    }
}
