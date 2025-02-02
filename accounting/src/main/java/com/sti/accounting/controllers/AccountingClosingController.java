package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountingClosingResponse;
import com.sti.accounting.services.AccountingClosingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/accounting-closing")
public class AccountingClosingController {

    private final AccountingClosingService accountingClosingService;

    public AccountingClosingController(AccountingClosingService accountingClosingService) {
        this.accountingClosingService = accountingClosingService;
    }

    @GetMapping()
    public List<AccountingClosingResponse> getAllAccountingClosing() {
        return accountingClosingService.getAllAccountingClosing();
    }

    @GetMapping("/detail")
    public AccountingClosingResponse getDetailAccountingClosing() {
        return accountingClosingService.getDetailAccountingClosing();
    }

    @PostMapping("/close")
    public ResponseEntity<String> closeAccountingPeriod(@RequestParam String newClosureType) {
        try {
            accountingClosingService.closeAccountingPeriod(newClosureType);
            return ResponseEntity.ok("Accounting period closed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error closing accounting period: " + e.getMessage());
        }
    }

    @GetMapping(value = "/{id}/download-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        // Obtener el cierre contable con el PDF generado
        AccountingClosingResponse closingResponse = accountingClosingService.getAccountingClosingById(id);

        // Obtener el PDF en formato byte[]
        byte[] pdfData = closingResponse.getClosureReportPdf();

        // Validar si el PDF existe
        if (pdfData == null || pdfData.length == 0) {
            return ResponseEntity.notFound().build();
        }

        // Configurar encabezados para la respuesta HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "closure-report-" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
    }

    @PostMapping("/annual-close")
    public ResponseEntity<String> closePeriodAnnualClosing(@RequestParam String newClosureType) {
        try {
            accountingClosingService.performAnnualClosing(newClosureType);
            return ResponseEntity.ok("Annual closing completed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during annual closing: " + e.getMessage());
        }
    }
}
