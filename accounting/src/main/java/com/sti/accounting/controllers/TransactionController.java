package com.sti.accounting.controllers;

import com.sti.accounting.models.BalanceGeneralResponse;
import com.sti.accounting.models.Constant;
import com.sti.accounting.entities.TransactionEntity;
import com.sti.accounting.entities.TransactionSumViewEntity;
import com.sti.accounting.models.TransactionByPeriodRequest;
import com.sti.accounting.models.TransactionRequest;
import com.sti.accounting.services.TransactionService;
import com.sti.accounting.utils.Util;
import jakarta.ws.rs.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
        transactionService.CreateTransaction(model);
    }

    @PostMapping
    public ResponseEntity<Object> CreateTransaction(@Validated @RequestBody TransactionRequest transactionRequest) {
        try {
            TransactionEntity newTransaction = transactionService.CreateTransaction(transactionRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(util.setSuccessResponse(newTransaction, HttpStatus.CREATED));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error creating transaction"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> UpdateTransaction(@PathVariable("id") Long id, @Validated @RequestBody TransactionRequest transactionRequest) {
        try {
            TransactionEntity updateTransaction = transactionService.UpdateTransaction(id, transactionRequest);

            return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(updateTransaction, HttpStatus.OK));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error updating transaction"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Object> ChangeTransactionStatus(@PathVariable("id") Long transactionId) {
        try {
            transactionService.ChangeTransactionStatus(transactionId);
            return ResponseEntity.status(HttpStatus.OK).body(util.setSuccessResponse(null, HttpStatus.OK));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error changing transaction status"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }


    @GetMapping("balance/general")
    public List<BalanceGeneralResponse> GetTrxByPeriod() {
        return transactionService.GetBalanceGeneral();
//        try {
//            return ResponseEntity.status(HttpStatus.OK).body(util.setSuccessResponse( transactionService.getBalanceGeneral(), HttpStatus.OK));
//        } catch (BadRequestException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error transaction By Period"));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
//        }
    }


    @PostMapping("byPeriodSum")
    public List<TransactionSumViewEntity> GetTrxByPeriodSum(@Validated @RequestBody TransactionByPeriodRequest transactionRequest) {
        return transactionService.GetTrxSum(transactionRequest);
//        try {
//
//            return ResponseEntity.status(HttpStatus.CREATED).body(util.setSuccessResponse(trx, HttpStatus.CREATED));
//        } catch (BadRequestException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error transaction By Period"));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
//        }
    }


}
