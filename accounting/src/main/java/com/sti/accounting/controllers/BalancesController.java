package com.sti.accounting.controllers;

import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.BalancesRequest;
import com.sti.accounting.services.BalancesService;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/balances")
public class BalancesController {

    private final BalancesService balancesService;


    public BalancesController(BalancesService balancesService) {
        this.balancesService = balancesService;
    }

    // Endpoint para obtener todos los saldos
    @GetMapping
    public List<BalancesRequest> getAllBalances() {
        return balancesService.GetAllBalances();

    }

    @GetMapping("/{id}")
    public BalancesRequest getBalanceById(@PathVariable Long id) {
        return balancesService.GetById(id);
    }

    //TODO: NO SE DEBE RETORNAR EL ENTITY
    @PostMapping
    public BalancesEntity createBalance(@Validated @RequestBody BalancesRequest balancesRequest) {
        return balancesService.CreateBalances(balancesRequest);

    }

    @PutMapping("/{id}")
    public BalancesEntity updateBalance(@PathVariable("id") Long id, @Validated @RequestBody BalancesRequest balancesRequest) {
        return balancesService.UpdateBalance(id, balancesRequest);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteBalance(@PathVariable("id") Long id) {
        balancesService.DeleteBalance(id);
        return ResponseEntity.noContent().build();
    }
}
