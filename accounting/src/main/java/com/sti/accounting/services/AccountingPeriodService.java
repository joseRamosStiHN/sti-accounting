package com.sti.accounting.services;


import com.sti.accounting.entities.AccountingPeriodEntity;

import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountingPeriodRepository;
import com.sti.accounting.utils.PeriodStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public List<AccountingPeriodResponse> getAccountingPeriodByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.trace("accounting period request with startDate {} and endDate {}", startDate, endDate);
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Invalid date range: start date %s cannot be after end date", startDate));
        }

        return accountingPeriodRepository.findByStartPeriodBetween(startDate, endDate).stream().map(this::toResponse).toList();
    }

    public AccountingPeriodResponse createAccountingPeriod(AccountingPeriodRequest accountingPeriodRequest) {
        List<AccountingPeriodEntity> periods = generatePeriods(accountingPeriodRequest);

        // Guardar todos los períodos en la base de datos
        accountingPeriodRepository.saveAll(periods);

        // El primer período es el activo
        AccountingPeriodEntity activePeriod = periods.get(0);
        activePeriod.setPeriodStatus(PeriodStatus.ACTIVE);
        accountingPeriodRepository.save(activePeriod);

        return toResponse(activePeriod);
    }

    private List<AccountingPeriodEntity> generatePeriods(AccountingPeriodRequest request) {
        List<AccountingPeriodEntity> periods = new ArrayList<>();

        LocalDateTime startDate = request.getStartPeriod();
        int months = getMonthsForClosureType(request.getClosureType());
        int numberOfPeriods = Math.ceilDiv(12, months);

        for (int i = 0; i < numberOfPeriods; i++) {
            AccountingPeriodEntity period = new AccountingPeriodEntity();

            period.setPeriodName(String.format("%s %d", request.getPeriodName(), i + 1));
            period.setClosureType(request.getClosureType());
            period.setStartPeriod(startDate);

            LocalDateTime endPeriod = startDate.plusMonths(months).minusDays(1);
            if (endPeriod.getYear() > startDate.getYear()) {
                endPeriod = LocalDateTime.of(startDate.getYear(), 12, 31, 23, 59, 59);
            }

            period.setEndPeriod(endPeriod);
            period.setDaysPeriod((int) java.time.Duration.between(startDate, endPeriod).toDays() + 1);
            period.setPeriodStatus(PeriodStatus.INACTIVE);
            period.setPeriodOrder(i + 1);
            period.setIsAnnual(request.getIsAnnual());

            periods.add(period);
            startDate = endPeriod.plusDays(1);
        }

        return periods;
    }

    private int getMonthsForClosureType(String closureType) {
        return switch (closureType.toLowerCase()) {
            case "mensual" -> 1;
            case "trimestral" -> 3;
            case "semestral" -> 6;
            case "anual" -> 12;
            default -> throw new IllegalArgumentException("Closure type not recognized: " + closureType);
        };
    }

    public AccountingPeriodResponse updateAccountingPeriod(Long id, AccountingPeriodRequest accountingPeriodRequest) {
        logger.info("Updating accounting period with ID: {}", id);

        AccountingPeriodEntity existingAccountingPeriod = accountingPeriodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No accounting period found with ID: %d", id)));

        // Desactivar los periodos que esten activos cuando se mande activar un periodo
        if (accountingPeriodRequest.getPeriodStatus() == PeriodStatus.ACTIVE) {
            List<AccountingPeriodEntity> activePeriods = accountingPeriodRepository.findActivePeriods();
            for (AccountingPeriodEntity activePeriod : activePeriods) {
                if (!activePeriod.getId().equals(existingAccountingPeriod.getId())) {
                    activePeriod.setPeriodStatus(PeriodStatus.INACTIVE);
                    accountingPeriodRepository.save(activePeriod);
                }
            }
        }

        existingAccountingPeriod.setPeriodName(accountingPeriodRequest.getPeriodName());
        existingAccountingPeriod.setClosureType(accountingPeriodRequest.getClosureType());
        existingAccountingPeriod.setStartPeriod(accountingPeriodRequest.getStartPeriod());
        existingAccountingPeriod.setEndPeriod(accountingPeriodRequest.getEndPeriod() == null ? calculateEndPeriod(accountingPeriodRequest.getStartPeriod(), accountingPeriodRequest.getClosureType()) : accountingPeriodRequest.getEndPeriod());
        existingAccountingPeriod.setDaysPeriod(accountingPeriodRequest.getDaysPeriod());
        existingAccountingPeriod.setPeriodStatus(accountingPeriodRequest.getPeriodStatus());
        existingAccountingPeriod.setPeriodOrder(accountingPeriodRequest.getPeriodOrder());

        existingAccountingPeriod.setIsAnnual(accountingPeriodRequest.getIsAnnual());
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
        return !accountingPeriodRepository.findActivePeriods().isEmpty();
    }

    private LocalDateTime calculateEndPeriod(LocalDateTime startPeriod, String closureType) {
        if (closureType.equalsIgnoreCase("Mensual")) {
            return startPeriod.plusMonths(1).minusDays(1);
        } else if (closureType.equalsIgnoreCase("trimestral")) {
            return startPeriod.plusMonths(3).minusDays(1);
        } else if (closureType.equalsIgnoreCase("semestral")) {
            return startPeriod.plusMonths(6).minusDays(1);
        } else if (closureType.equalsIgnoreCase("anual")) {
            return startPeriod.plusYears(1).minusDays(1);
        } else if (closureType.equalsIgnoreCase("semanal")) {
            return startPeriod.plusWeeks(1).minusDays(1);
        } else {
            throw new IllegalArgumentException("Closure type not recognized: " + closureType);
        }
    }

    @Cacheable("activePeriod")
    public AccountingPeriodEntity getActivePeriod() {
        return accountingPeriodRepository.findActivePeriods()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no active accounting period"));
    }

    public List<AccountingPeriodEntity> getClosedPeriods() {
        return accountingPeriodRepository.findByPeriodStatus();
    }

    private AccountingPeriodResponse toResponse(AccountingPeriodEntity entity) {
        AccountingPeriodResponse response = new AccountingPeriodResponse();

        response.setId(entity.getId());
        response.setPeriodName(entity.getPeriodName());
        response.setClosureType(entity.getClosureType());
        response.setStartPeriod(entity.getStartPeriod());
        response.setEndPeriod(entity.getEndPeriod());
        response.setDaysPeriod(entity.getDaysPeriod());
        response.setPeriodStatus(entity.getPeriodStatus().toString());
        response.setPeriodOrder(entity.getPeriodOrder());
        response.setIsAnnual(entity.getIsAnnual());
        return response;
    }


}
