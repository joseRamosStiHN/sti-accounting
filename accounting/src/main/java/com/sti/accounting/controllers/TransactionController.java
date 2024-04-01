package com.sti.accounting.controllers;

import com.sti.accounting.models.TransactionRequest;
import com.sti.accounting.services.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void addTransaction(@RequestBody TransactionRequest model){
        transactionService.AddTransaction(model);
    }



}
