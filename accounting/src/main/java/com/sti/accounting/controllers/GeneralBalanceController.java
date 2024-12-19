package com.sti.accounting.controllers;

import com.sti.accounting.models.GeneralBalanceResponse;
import com.sti.accounting.services.GeneralBalanceService;
import org.springframework.web.bind.annotation.*;

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
    public List<GeneralBalanceResponse> getBalanceGeneral(@RequestParam(required = false) Long periodId) {
        return generalBalanceService.getBalanceGeneral(periodId);
    }
}
