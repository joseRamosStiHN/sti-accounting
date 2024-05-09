package com.sti.accounting.controllers;

import com.sti.accounting.models.BalanceGeneralResponse;
import com.sti.accounting.services.BalanceService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/balance")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }


    @GetMapping("/general")
    public List<BalanceGeneralResponse> getBalanceGeneral() {
        return balanceService.getBalanceGeneral();
    }

}
