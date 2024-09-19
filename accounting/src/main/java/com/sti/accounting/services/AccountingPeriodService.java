package com.sti.accounting.services;


import com.sti.accounting.entities.AccountingPeriodEntity;

import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountingPeriodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

        entity.setPeriodName(accountingPeriodRequest.getPeriodName());
        entity.setClosureType(accountingPeriodRequest.getClosureType());
        entity.setStartPeriod(accountingPeriodRequest.getStartPeriod());
        entity.setEndPeriod(accountingPeriodRequest.getEndPeriod() == null ? calculateEndPeriod(accountingPeriodRequest.getStartPeriod(), accountingPeriodRequest.getClosureType()) : accountingPeriodRequest.getEndPeriod());
        entity.setDaysPeriod(accountingPeriodRequest.getDaysPeriod());
        entity.setStatus(false);
        accountingPeriodRepository.save(entity);

        return toResponse(entity);
    }

    public AccountingPeriodResponse updateAccountingPeriod(Long id, AccountingPeriodRequest accountingPeriodRequest) {
        logger.info("Updating accounting period with ID: {}", id);

        AccountingPeriodEntity existingAccountingPeriod = accountingPeriodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No accounting period found with ID: %d", id)));

        // Desactivar los periodos que esten activos cuando se mande activar un periodo
        if (accountingPeriodRequest.isStatus()) {
            List<AccountingPeriodEntity> activePeriods = accountingPeriodRepository.findAllByStatus(true);
            for (AccountingPeriodEntity activePeriod : activePeriods) {
                if (!activePeriod.getId().equals(existingAccountingPeriod.getId())) {
                    activePeriod.setStatus(false);
                    accountingPeriodRepository.save(activePeriod);
                }
            }
        }
        existingAccountingPeriod.setPeriodName(accountingPeriodRequest.getPeriodName());
        existingAccountingPeriod.setClosureType(accountingPeriodRequest.getClosureType());
        existingAccountingPeriod.setStartPeriod(accountingPeriodRequest.getStartPeriod());
        existingAccountingPeriod.setEndPeriod(accountingPeriodRequest.getEndPeriod() == null ? calculateEndPeriod(accountingPeriodRequest.getStartPeriod(), accountingPeriodRequest.getClosureType()) : accountingPeriodRequest.getEndPeriod());
        existingAccountingPeriod.setDaysPeriod(accountingPeriodRequest.getDaysPeriod());
        existingAccountingPeriod.setStatus(accountingPeriodRequest.isStatus());

        accountingPeriodRepository.save(existingAccountingPeriod);

        return toResponse(existingAccountingPeriod);
    }

    public AccountingPeriodResponse deleteAccountingPeriod(Long id) {
        logger.info("Deleting accounting period with ID: {}", id);

        AccountingPeriodEntity existingAccountingPeriod = accountingPeriodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No accounting period found with ID: %d", id)));

        accountingPeriodRepository.delete(existingAccountingPeriod);
        return toResponse(existingAccountingPeriod);
    }

    public boolean isActivePeriodExists() {
        return accountingPeriodRepository.findByStatusTrue().isPresent();
    }

    private LocalDateTime calculateEndPeriod(LocalDateTime startPeriod, String closureType) {
        if (closureType.equalsIgnoreCase("mensual")) {
            return startPeriod.plusMonths(1);
        } else if (closureType.equalsIgnoreCase("trimestral")) {
            return startPeriod.plusMonths(3);
        } else if (closureType.equalsIgnoreCase("semestral")) {
            return startPeriod.plusMonths(6);
        } else if (closureType.equalsIgnoreCase("anual")) {
            return startPeriod.plusYears(1);
        } else if (closureType.equalsIgnoreCase("semanal")) {
            return startPeriod.plusWeeks(1);
        } else {
            throw new IllegalArgumentException("Closure type not recognized: " + closureType);
        }
    }

    @Cacheable("activePeriod")
    public AccountingPeriodEntity getActivePeriod() {
        return accountingPeriodRepository.findByStatusTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no active accounting period"));
    }

    public LocalDate getDateStartPeriodAccountingActive() {
        return getActivePeriod().getStartPeriod().toLocalDate();
    }

    public LocalDate getActiveAccountingPeriodEndDate() {
        return getActivePeriod().getEndPeriod().toLocalDate();
    }

    private AccountingPeriodResponse toResponse(AccountingPeriodEntity entity) {
        AccountingPeriodResponse response = new AccountingPeriodResponse();

        response.setId(entity.getId());
        response.setPeriodName(entity.getPeriodName());
        response.setClosureType(entity.getClosureType());
        response.setStartPeriod(entity.getStartPeriod());
        response.setEndPeriod(entity.getEndPeriod());
        response.setDaysPeriod(entity.getDaysPeriod());
        response.setStatus(entity.isStatus());
        return response;
    }


}
