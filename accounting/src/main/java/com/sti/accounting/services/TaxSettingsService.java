package com.sti.accounting.services;

import com.sti.accounting.entities.TaxSettingsEntity;
import com.sti.accounting.models.TaxSettingsRequest;
import com.sti.accounting.models.TaxSettingsResponse;
import com.sti.accounting.repositories.ITaxSettingsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TaxSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(TaxSettingsService.class);
    private final ITaxSettingsRepository taxSettingsRepository;
    private final AuthService authService;

    public TaxSettingsService(ITaxSettingsRepository taxSettingsRepository, AuthService authService) {
        this.taxSettingsRepository = taxSettingsRepository;
        this.authService = authService;
    }

    public List<TaxSettingsResponse> getAllTaxSettings() {
        String tenantId = authService.getTenantId();
        return this.taxSettingsRepository.findAll().stream().filter(tax -> tax.getTenantId().equals(tenantId)).map(this::toResponse).toList();
    }

    public TaxSettingsResponse getTaxSettingsById(Long id) {
        logger.trace("Tax Settings request with id {}", id);
        String tenantId = authService.getTenantId();

        TaxSettingsEntity taxSettingsEntity = taxSettingsRepository.findById(id).filter(tax -> tax.getTenantId().equals(tenantId)).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("No tax settings were found with the id %s", id))
        );
        return toResponse(taxSettingsEntity);
    }

    public TaxSettingsResponse createTaxSettings(TaxSettingsRequest taxSettingsRequest) {
        String tenantId = authService.getTenantId();

        TaxSettingsEntity taxSettingsEntity = new TaxSettingsEntity();

        taxSettingsEntity.setTaxRate(taxSettingsRequest.getTaxRate());
        taxSettingsEntity.setType(taxSettingsRequest.getType());
        taxSettingsEntity.setFromValue(taxSettingsRequest.getFromValue());
        taxSettingsEntity.setToValue(taxSettingsRequest.getToValue());
        taxSettingsEntity.setIsCurrent(taxSettingsRequest.getIsCurrent());
        taxSettingsEntity.setTenantId(tenantId);
        taxSettingsRepository.save(taxSettingsEntity);
        return toResponse(taxSettingsEntity);
    }

    public TaxSettingsResponse updateTaxSettings(Long id, TaxSettingsRequest taxSettingsRequest) {
        String tenantId = authService.getTenantId();

        TaxSettingsEntity taxSettingsEntity = taxSettingsRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                String.format("No ta settings found with ID: %d", id)));

        taxSettingsEntity.setTaxRate(taxSettingsRequest.getTaxRate());
        taxSettingsEntity.setType(taxSettingsRequest.getType());
        taxSettingsEntity.setFromValue(taxSettingsRequest.getFromValue());
        taxSettingsEntity.setToValue(taxSettingsRequest.getToValue());
        taxSettingsEntity.setIsCurrent(taxSettingsRequest.getIsCurrent());
        taxSettingsEntity.setTenantId(tenantId);

        taxSettingsRepository.save(taxSettingsEntity);
        return toResponse(taxSettingsEntity);
    }

    public BigDecimal getTaxRateForUtility(BigDecimal utilityBeforeIsv, String taxType) {
        String tenantId = authService.getTenantId();

        return taxSettingsRepository.findAll().stream()
                .filter(tax ->
                        tenantId.equals(tax.getTenantId()) &&
                                taxType.equalsIgnoreCase(tax.getType()) &&
                                utilityBeforeIsv.compareTo(nullSafe(tax.getFromValue())) >= 0 &&
                                (tax.getToValue() == null || utilityBeforeIsv.compareTo(tax.getToValue()) <= 0)
                )
                .map(tax -> parseRateToDecimal(tax.getTaxRate()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No tax rate found for the given utility value and scope (" + taxType + ")"
                ));
    }

    private static BigDecimal parseRateToDecimal(String raw) {
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tax rate is null");
        }

        String s = raw.trim();

        // Exenciones
        if (s.equalsIgnoreCase("exento") ||
                s.equalsIgnoreCase("exentos") ||
                s.equalsIgnoreCase("exenta")) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Detectar y quitar el símbolo %
        boolean hadPercent = s.endsWith("%");
        if (hadPercent) {
            s = s.substring(0, s.length() - 1).trim();
        }

        // Normalizar coma decimal a punto
        s = s.replace(',', '.');

        // Validar formato numérico
        if (!s.matches("^[+-]?\\d+(\\.\\d+)?$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid tax rate format: '" + raw + "'. Use 25, 25%, 0.25 or 'Exentos'.");
        }

        BigDecimal val = new BigDecimal(s);

        // Interpretar valor
        BigDecimal rate = hadPercent
                ? val.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : (val.compareTo(BigDecimal.ONE) > 0
                ? val.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : val);

        return rate.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }


    private TaxSettingsResponse toResponse(TaxSettingsEntity entity) {
        TaxSettingsResponse response = new TaxSettingsResponse();

        response.setId(entity.getId());
        response.setTaxRate(entity.getTaxRate());
        response.setType(entity.getType());
        response.setFromValue(entity.getFromValue());
        response.setToValue(entity.getToValue());
        response.setIsCurrent(entity.getIsCurrent());
        response.setCreationDate(entity.getCreateAtDate());
        return response;
    }
}
