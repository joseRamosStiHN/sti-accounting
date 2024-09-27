package com.sti.accounting.controllers;

import com.sti.accounting.models.GeneralBalanceResponse;
import com.sti.accounting.services.GeneralBalanceService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/balance")
public class GeneralBalanceController {

    private final GeneralBalanceService generalBalanceService;

    public GeneralBalanceController(GeneralBalanceService generalBalanceService) {
        this.generalBalanceService = generalBalanceService;
    }


    @GetMapping("/general")
    public List<GeneralBalanceResponse> getBalanceGeneral() {
        return generalBalanceService.getBalanceGeneral();
    }

}
