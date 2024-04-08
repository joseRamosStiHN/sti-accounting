package com.sti.accounting.controllers;

import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.entities.TransactionEntity;
import com.sti.accounting.models.BalancesRequest;
import com.sti.accounting.models.TransactionRequest;
import com.sti.accounting.services.TransactionService;
import com.sti.accounting.utils.Util;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transaction")
public class TransactionController {

    private final TransactionService transactionService;
    private final Util util;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
        this.util = new Util();

    }

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void addTransaction(@RequestBody TransactionRequest model) {
        transactionService.createTransaction(model);
    }

    @PostMapping
    public ResponseEntity<Object> createTransaction(@Valid @RequestBody TransactionRequest transactionRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setValidationError(bindingResult));
        }
        try {
            TransactionEntity newTransaction = transactionService.createTransaction(transactionRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(util.setSuccessResponse(newTransaction, HttpStatus.CREATED));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error creating transaction"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateTransaction(@PathVariable("id") Long id, @Valid @RequestBody TransactionRequest transactionRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setValidationError(bindingResult));
        }
        try {
            TransactionEntity updateTransaction = transactionService.updateTransaction(id, transactionRequest);

            return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(updateTransaction, HttpStatus.OK));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error updating transaction"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Object> changeTransactionStatus(@PathVariable("id") Long transactionId) {
        try {
            transactionService.changeTransactionStatus(transactionId);
            return ResponseEntity.status(HttpStatus.OK).body(util.setSuccessResponse(null, HttpStatus.OK));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error changing transaction status"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e.getMessage()));
        }
    }
}
