package com.sti.accounting.controllers;

import com.sti.accounting.services.ReportService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/report/balance")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService balanceService) {
        this.reportService = balanceService;
    }


    /*
    @GetMapping("/general")
    public List<BalanceGeneralResponse> getBalanceGeneral() {
        return reportService.getBalanceGeneral();
    }
*/
}
