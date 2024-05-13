package com.sti.accounting.controllers;

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
        return transactionService.getAllTransaction();
    }

    @GetMapping("/{id}")
    public TransactionResponse GetTransactionById(@PathVariable("id") Long id) {
        return transactionService.getById(id);
    }

    @GetMapping("/by-document/{id}")
    public List<TransactionResponse> GetTransactionByDocumentType(@PathVariable("id") Long id) {
        return transactionService.getByDocumentType(id);
    }

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void addTransaction(@RequestBody TransactionRequest model) {
        transactionService.createTransaction(model);
    }

    @PostMapping
    public TransactionResponse CreateTransaction(@Validated @RequestBody TransactionRequest transactionRequest) {
        return transactionService.createTransaction(transactionRequest);
    }

    @PutMapping("/{id}")
    public TransactionResponse UpdateTransaction(@PathVariable("id") Long id, @Validated @RequestBody TransactionRequest transactionRequest) {
        return transactionService.updateTransaction(id, transactionRequest);
    }

    @PutMapping("/{id}/post")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ChangeTransactionStatus(@PathVariable("id") Long transactionId) {
        transactionService.changeTransactionStatus(transactionId);
    }
}
