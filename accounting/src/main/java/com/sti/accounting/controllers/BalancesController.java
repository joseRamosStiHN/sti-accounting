package com.sti.accounting.controllers;

import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.BalancesRequest;
import com.sti.accounting.services.BalancesService;
import com.sti.accounting.utils.Util;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        BalancesRequest balancesRequest = balancesService.getById(id);
        return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(balancesRequest, HttpStatus.OK));

    }

    @PostMapping
    public ResponseEntity<Object> createBalance(@Valid @RequestBody BalancesRequest balancesRequest) {
        BalancesEntity newBalance = balancesService.createBalances(balancesRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.util.setSuccessResponse(newBalance, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateBalance(@PathVariable("id") Long id, @Valid @RequestBody BalancesRequest balancesRequest) {
        BalancesEntity updatedBalance = balancesService.updateBalance(id, balancesRequest);
        return ResponseEntity.status(HttpStatus.OK).body(this.util.setSuccessResponse(updatedBalance, HttpStatus.OK));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteBalance(@PathVariable("id") Long id) {
        balancesService.deleteBalance(id);
        return ResponseEntity.noContent().build();
    }
}
