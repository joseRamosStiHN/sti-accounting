package com.sti.accounting.services;


import com.sti.accounting.entities.AccountingPeriodEntity;

import com.sti.accounting.models.AccountingPeriodRequest;
import com.sti.accounting.models.AccountingPeriodResponse;
import com.sti.accounting.repositories.IAccountingPeriodRepository;
import com.sti.accounting.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccountingPeriodService {

    private static final Logger logger = LoggerFactory.getLogger(AccountingPeriodService.class);
    private final IAccountingPeriodRepository accountingPeriodRepository;

    public AccountingPeriodService(IAccountingPeriodRepository accountingPeriodRepository) {
        this.accountingPeriodRepository = accountingPeriodRepository;
    }

    public List<AccountingPeriodResponse> getAllAccountingPeriod() {
        return this.accountingPeriodRepository.findAll().stream().map(this::toResponse).toList();
    }

    public AccountingPeriodResponse getById(Long id) {
        logger.trace("accounting period request with id {}", id);
        AccountingPeriodEntity accountingPeriodEntity = accountingPeriodRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("No accounting period were found with the id %s", id))
        );
        return toResponse(accountingPeriodEntity);
    }

    public AccountingPeriodResponse createAccountingPeriod(AccountingPeriodRequest accountingPeriodRequest) {
        AccountingPeriodEntity entity = new AccountingPeriodEntity();

        entity.setDescription(accountingPeriodRequest.getDescription());
        entity.setClosureType(accountingPeriodRequest.getClosureType());
        entity.setStartDate(accountingPeriodRequest.getStartDate());
        entity.setEndDate(accountingPeriodRequest.getEndDate());
        entity.setStatus(accountingPeriodRequest.getStatus());
        accountingPeriodRepository.save(entity);

        return toResponse(entity);
    }

    public AccountingPeriodResponse updateAccountingPeriod(Long id, AccountingPeriodRequest accountingPeriodRequest) {
        logger.info("Updating accounting period with ID: {}", id);

        AccountingPeriodEntity existingAccountingPeriod = accountingPeriodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No accounting period found with ID: %d", id)));

        existingAccountingPeriod.setDescription(accountingPeriodRequest.getDescription());
        existingAccountingPeriod.setClosureType(accountingPeriodRequest.getClosureType());
        existingAccountingPeriod.setStartDate(accountingPeriodRequest.getStartDate());
        existingAccountingPeriod.setEndDate(accountingPeriodRequest.getEndDate());
        existingAccountingPeriod.setStatus(accountingPeriodRequest.getStatus());

        accountingPeriodRepository.save(existingAccountingPeriod);

        return toResponse(existingAccountingPeriod);
    }

    private AccountingPeriodResponse toResponse(AccountingPeriodEntity entity) {
        AccountingPeriodResponse response = new AccountingPeriodResponse();

        response.setId(entity.getId());
        response.setDescription(entity.getDescription());
        response.setClosureType(entity.getClosureType());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        response.setStatus(entity.getStatus());
        return response;
    }
}
