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
                .filter(tax -> tax.getTenantId().equals(tenantId) &&
                        tax.getType().equals(taxType) &&
                        utilityBeforeIsv.compareTo(tax.getFromValue()) >= 0 &&
                        (tax.getToValue() == null || utilityBeforeIsv.compareTo(tax.getToValue()) <= 0))
                .map(tax -> {
                    try {
                        return new BigDecimal(tax.getTaxRate())
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    } catch (NumberFormatException e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid tax rate format");
                    }
                })
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tax rate found for the given utility value"));
    }

    private TaxSettingsResponse toResponse(TaxSettingsEntity entity) {
        TaxSettingsResponse response = new TaxSettingsResponse();

        response.setTaxRate(entity.getTaxRate());
        response.setType(entity.getType());
        response.setFromValue(entity.getFromValue());
        response.setToValue(entity.getToValue());
        response.setIsCurrent(entity.getIsCurrent());
        response.setCreationDate(entity.getCreateAtDate());
        return response;
    }
}
