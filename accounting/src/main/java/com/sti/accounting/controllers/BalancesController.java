package com.sti.accounting.controllers;

import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.BalancesRequest;
import com.sti.accounting.services.BalancesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/balances")
public class BalancesController {

    private final BalancesService balancesService;

    public BalancesController(BalancesService balancesService) {
        this.balancesService = balancesService;
    }

    // Endpoint para obtener todos los saldos
    @GetMapping
    public ResponseEntity<Object> getAllBalances() {
        List<BalancesRequest> balances = balancesService.getAllBalances();
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BalancesRequest> getBalanceById(@PathVariable Long id) {
        BalancesRequest balancesRequest;
        try {
            balancesRequest = balancesService.getById(id);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
        return ResponseEntity.ok(balancesRequest);
    }

    @PostMapping
    public ResponseEntity<Object> createBalance(@RequestBody BalancesRequest balancesRequest) {
        BalancesEntity newBalance = balancesService.createBalances(balancesRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(newBalance);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateBalance(@PathVariable("id") Long id,@Valid @RequestBody BalancesRequest balancesRequest) {
        BalancesEntity updatedBalance = balancesService.updateBalance(id, balancesRequest);
        if (updatedBalance != null) {
            return ResponseEntity.ok(updatedBalance);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteBalance(@PathVariable("id") Long id) {
        balancesService.deleteBalance(id);
        return ResponseEntity.noContent().build();
    }
}
