package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountingAdjustmentRequest;
import com.sti.accounting.models.AccountingAdjustmentResponse;
import com.sti.accounting.services.AccountingAdjustmentService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
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

    @PostMapping
    public AccountingAdjustmentResponse createAdjustment(@Validated @RequestBody AccountingAdjustmentRequest accountingAdjustmentRequest) {
        return accountingAdjustmentService.createAdjustment(accountingAdjustmentRequest);
    }

    @PutMapping("/{id}/post")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeTransactionStatus(@PathVariable("id") Long adjustmentId) {
        accountingAdjustmentService.changeAdjustmentStatus(adjustmentId);
    }
}
