package com.sti.accounting.controllers;


import com.sti.accounting.models.BalancesRequest;
import com.sti.accounting.models.BalancesResponse;
import com.sti.accounting.services.BalancesService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/v1/balances")
public class BalancesController {

    private final BalancesService balancesService;


    public BalancesController(BalancesService balancesService) {
        this.balancesService = balancesService;
    }

    @GetMapping()
    public List<BalancesResponse> getAllBalances() {
        return balancesService.getAllBalances();
    }


    @GetMapping("/{id}")
    public BalancesResponse getBalanceById(@PathVariable Long id) {
        return balancesService.getById(id);
    }

    @PostMapping
    public BalancesResponse createBalance(@Validated @RequestBody BalancesRequest balancesRequest) {
        return balancesService.createBalance(balancesRequest);

    }

    @PutMapping("/{id}")
    public BalancesResponse updateBalance(@PathVariable("id") Long id, @Validated @RequestBody BalancesRequest balancesRequest) {
        return balancesService.updateBalance(id, balancesRequest);

    }

}
