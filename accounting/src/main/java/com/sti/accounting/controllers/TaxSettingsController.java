package com.sti.accounting.controllers;


import com.sti.accounting.models.TaxSettingsRequest;
import com.sti.accounting.models.TaxSettingsResponse;
import com.sti.accounting.services.TaxSettingsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


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

    @GetMapping("/rate")
    public Map<String, BigDecimal> getRate(
            @RequestParam(name = "scope", defaultValue = "Mensual") String scope,
            @RequestParam(name = "uai") BigDecimal utilidadAntesImpuestos) {

        final String type = "Anual".equalsIgnoreCase(scope)
                ? "Renta Gravable Anual"
                : "Renta Gravable Mensual";

        BigDecimal rate = taxSettingsService.getTaxRateForUtility(utilidadAntesImpuestos, type);
        return Map.of("rate", rate);
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
