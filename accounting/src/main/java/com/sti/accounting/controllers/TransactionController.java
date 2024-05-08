package com.sti.accounting.controllers;

import com.sti.accounting.models.BalanceGeneralResponse;
import com.sti.accounting.entities.TransactionSumViewEntity;
import com.sti.accounting.models.TransactionByPeriodRequest;
import com.sti.accounting.models.TransactionRequest;
import com.sti.accounting.models.TransactionResponse;
import com.sti.accounting.services.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionResponse> getAllTransactions() {
        return transactionService.GetAllTransaction();
    }

    @GetMapping("/{id}")
    public TransactionResponse GetTransactionById(@PathVariable("id") Long id) {
        return transactionService.GetById(id);
    }


    @PostMapping("/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void addTransaction(@RequestBody TransactionRequest model) {
        transactionService.CreateTransaction(model);
    }

    @PostMapping
    public TransactionResponse CreateTransaction(@Validated @RequestBody TransactionRequest transactionRequest) {
        return transactionService.CreateTransaction(transactionRequest);
    }

    @PutMapping("/{id}")
    public TransactionResponse UpdateTransaction(@PathVariable("id") Long id, @Validated @RequestBody TransactionRequest transactionRequest) {
        return transactionService.UpdateTransaction(id, transactionRequest);
    }

    @PutMapping("/{id}/post")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ChangeTransactionStatus(@PathVariable("id") Long transactionId) {
        transactionService.ChangeTransactionStatus(transactionId);
    }


    @GetMapping("balance/general")
    public List<BalanceGeneralResponse> GetTrxByPeriod() {
        return transactionService.GetBalanceGeneral();
    }


    @PostMapping("byPeriodSum")
    public List<TransactionSumViewEntity> GetTrxByPeriodSum(@Validated @RequestBody TransactionByPeriodRequest transactionRequest) {
        return transactionService.GetTrxSum(transactionRequest);

    }


}
