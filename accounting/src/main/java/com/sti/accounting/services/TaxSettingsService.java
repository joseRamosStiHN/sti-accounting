package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingJournalEntity;
import com.sti.accounting.entities.TaxSettingsEntity;
import com.sti.accounting.models.AccountingJournalResponse;
import com.sti.accounting.models.TaxSettingsRequest;
import com.sti.accounting.models.TaxSettingsResponse;
import com.sti.accounting.repositories.ITaxSettingsRepository;
import com.sti.accounting.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaxSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(TaxSettingsService.class);
    private final ITaxSettingsRepository taxSettingsRepository;

    public TaxSettingsService(ITaxSettingsRepository taxSettingsRepository) {
        this.taxSettingsRepository = taxSettingsRepository;
    }


    private String getTenantId() {
        return TenantContext.getCurrentTenant();
    }


    public List<TaxSettingsResponse> getAllTaxSettings() {
        String tenantId = getTenantId();
        return this.taxSettingsRepository.findAll().stream().filter(tax -> tax.getTenantId().equals(tenantId)).map(this::toResponse).toList();
    }

    public TaxSettingsResponse getTaxSettingsById(Long id) {
        logger.trace("Tax Settings request with id {}", id);
        String tenantId = getTenantId();

        TaxSettingsEntity taxSettingsEntity = taxSettingsRepository.findById(id).filter(tax -> tax.getTenantId().equals(tenantId)).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("No tax settings were found with the id %s", id))
        );
        return toResponse(taxSettingsEntity);
    }

    public TaxSettingsResponse createTaxSettings(TaxSettingsRequest taxSettingsRequest) {
        String tenantId = getTenantId();

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
        String tenantId = getTenantId();

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
