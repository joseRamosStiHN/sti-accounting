package com.sti.accounting.controllers;

import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.Constant;
import com.sti.accounting.models.BalancesRequest;
import com.sti.accounting.services.BalancesService;
import com.sti.accounting.utils.Util;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/balances")
public class BalancesController {

    private final BalancesService balancesService;
    private final Util util;

    public BalancesController(BalancesService balancesService) {
        this.balancesService = balancesService;
        this.util = new Util();
    }

    // Endpoint para obtener todos los saldos
    @GetMapping
    public ResponseEntity<Object> getAllBalances() {
        List<BalancesRequest> balances = balancesService.getAllBalances();
        return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(balances, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getBalanceById(@PathVariable Long id) {
        try {
            BalancesRequest balancesRequest = balancesService.getById(id);
            return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(balancesRequest, HttpStatus.OK));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error get Balance by Id"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<Object> createBalance(@Valid @RequestBody BalancesRequest balancesRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setValidationError(bindingResult));
        }
        try {
            BalancesEntity newBalance = balancesService.createBalances(balancesRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(this.util.setSuccessResponse(newBalance, HttpStatus.CREATED));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error creating balance"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateBalance(@PathVariable("id") Long id, @Valid @RequestBody BalancesRequest balancesRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setValidationError(bindingResult));
        }
        try {
            BalancesEntity updatedBalance = balancesService.updateBalance(id, balancesRequest);
            return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(updatedBalance, HttpStatus.OK));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(util.setError(HttpStatus.BAD_REQUEST, e.getMessage(), "Error updating balance"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(util.setError(HttpStatus.INTERNAL_SERVER_ERROR, Constant.ERROR_INTERNAL, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteBalance(@PathVariable("id") Long id) {
        balancesService.deleteBalance(id);
        return ResponseEntity.noContent().build();
    }
}
