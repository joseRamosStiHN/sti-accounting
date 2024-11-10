package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountingPeriodRequest;
import com.sti.accounting.models.AccountingPeriodResponse;
import com.sti.accounting.models.TaxSettingsRequest;
import com.sti.accounting.models.TaxSettingsResponse;
import com.sti.accounting.services.TaxSettingsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/tax-settings")
public class TaxSettingsController {

    private final TaxSettingsService taxSettingsService;

    public TaxSettingsController(final TaxSettingsService taxSettingsService) {
        this.taxSettingsService = taxSettingsService;
    }

    @GetMapping()
    public List<TaxSettingsResponse> getAllTaxSettings() {
        return taxSettingsService.getAllTaxSettings();
    }

    @GetMapping("/{id}")
    public TaxSettingsResponse getTaxSettingsById(@PathVariable Long id) {
        return taxSettingsService.getTaxSettingsById(id);
    }

    @PostMapping
    public TaxSettingsResponse createTaxSettings(@Validated @RequestBody TaxSettingsRequest taxSettingsRequest) {
        return taxSettingsService.createTaxSettings(taxSettingsRequest);
    }

    @PutMapping("/{id}")
    public TaxSettingsResponse updateTaxSettings(@PathVariable("id") Long id, @Validated @RequestBody TaxSettingsRequest taxSettingsRequest) {
        return taxSettingsService.updateTaxSettings(id, taxSettingsRequest);
    }
}
