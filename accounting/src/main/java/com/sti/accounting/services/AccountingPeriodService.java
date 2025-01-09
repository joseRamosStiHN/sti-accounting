package com.sti.accounting.services;


import com.sti.accounting.entities.AccountingPeriodEntity;

import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountingPeriodRepository;
import com.sti.accounting.utils.PeriodStatus;
import com.sti.accounting.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountingPeriodService {

    // Constantes para ClosureType
    private static final String CLOSURE_TYPE_MENSUAL = "mensual";
    private static final String CLOSURE_TYPE_TRIMESTRAL = "trimestral";
    private static final String CLOSURE_TYPE_SEMESTRAL = "semestral";
    private static final String CLOSURE_TYPE_ANUAL = "anual";

    private static final Logger logger = LoggerFactory.getLogger(AccountingPeriodService.class);
    private final IAccountingPeriodRepository accountingPeriodRepository;

    public AccountingPeriodService(IAccountingPeriodRepository accountingPeriodRepository) {
        this.accountingPeriodRepository = accountingPeriodRepository;
    }

    private String getTenantId() {
        return TenantContext.getCurrentTenant();
    }

    public List<AccountingPeriodResponse> getAllAccountingPeriod() {
        int currentYear = LocalDate.now().getYear();
        String tenantId = getTenantId();

        return accountingPeriodRepository.findAll().stream()
                .filter(period -> period.getTenantId().equals(tenantId))
                .filter(period -> period.getStartPeriod().getYear() == currentYear || period.getEndPeriod().getYear() == currentYear)
                .map(this::toResponse)
                .toList();
    }

    public AccountingPeriodResponse getById(Long id) {
        logger.trace("accounting period request with id {}", id);
        String tenantId = getTenantId();

        AccountingPeriodEntity accountingPeriodEntity = accountingPeriodRepository.findById(id)
                .filter(period -> period.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No accounting period were found with the id %s", id)));
        return toResponse(accountingPeriodEntity);
    }


    public List<AccountingPeriodResponse> getAccountingPeriodByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.trace("accounting period request with startDate {} and endDate {}", startDate, endDate);
        String tenantId = getTenantId();

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Invalid date range: start date %s cannot be after end date", startDate));
        }

        return accountingPeriodRepository.findByStartPeriodBetweenAndTenantId(startDate, endDate, tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountingPeriodResponse createAccountingPeriod(AccountingPeriodRequest accountingPeriodRequest) {

        String tenantId = getTenantId();

        if (isAccountingPeriodExists(accountingPeriodRequest)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is already an equal accounting period.");
        }

        boolean isAnnualPeriod = accountingPeriodRequest.getIsAnnual() != null && accountingPeriodRequest.getIsAnnual();

        if (!isAnnualPeriod) {
            List<AccountingPeriodEntity> activePeriods = accountingPeriodRepository.findActivePeriods(tenantId);
            for (AccountingPeriodEntity activePeriod : activePeriods) {
                activePeriod.setPeriodStatus(PeriodStatus.INACTIVE);
                accountingPeriodRepository.save(activePeriod);
            }
        }

        List<AccountingPeriodEntity> periods = generatePeriods(accountingPeriodRequest);
        periods.forEach(period -> period.setTenantId(tenantId));

        accountingPeriodRepository.saveAll(periods);

        AccountingPeriodEntity activePeriod = periods.get(0);
        activePeriod.setPeriodStatus(accountingPeriodRequest.getPeriodStatus() == null ? PeriodStatus.ACTIVE : accountingPeriodRequest.getPeriodStatus());
        accountingPeriodRepository.save(activePeriod);

        return toResponse(activePeriod);
    }

    public List<AccountingPeriodEntity> generatePeriods(AccountingPeriodRequest request) {
        List<AccountingPeriodEntity> periods = new ArrayList<>();

        LocalDateTime startDate = request.getStartPeriod();
        LocalDate endOfYear = startDate.toLocalDate().with(TemporalAdjusters.lastDayOfYear());

        while (startDate.toLocalDate().isBefore(endOfYear.plusDays(1))) {
            AccountingPeriodEntity period = new AccountingPeriodEntity();

            period.setPeriodName(String.format("%s %d", request.getPeriodName(), periods.size() + 1));
            period.setClosureType(request.getClosureType());
            period.setStartPeriod(startDate);

            LocalDate endPeriod;
            switch (request.getClosureType().toLowerCase()) {
                case CLOSURE_TYPE_MENSUAL:
                    // Fin del mes actual
                    endPeriod = startDate.toLocalDate().with(TemporalAdjusters.lastDayOfMonth());
                    break;

                case CLOSURE_TYPE_TRIMESTRAL:
                    // Calcular el fin del trimestre como 3 meses desde el inicio, pero limitado al fin del año
                    endPeriod = startDate.toLocalDate().plusMonths(3).withDayOfMonth(1).minusDays(1);
                    if (endPeriod.isAfter(endOfYear)) {
                        endPeriod = endOfYear;
                    }
                    break;

                case CLOSURE_TYPE_SEMESTRAL:
                    // Calcular el fin del semestre como 6 meses desde el inicio, pero limitado al fin del año
                    endPeriod = startDate.toLocalDate().plusMonths(6).withDayOfMonth(1).minusDays(1);
                    if (endPeriod.isAfter(endOfYear)) {
                        endPeriod = endOfYear;
                    }
                    break;

                case CLOSURE_TYPE_ANUAL:
                    // Fin del año
                    endPeriod = endOfYear;
                    break;

                default:
                    throw new IllegalArgumentException("Tipo de cierre no soportado: " + request.getClosureType());
            }

            period.setEndPeriod(endPeriod.atTime(23, 59, 59));
            period.setDaysPeriod((int) Duration.between(period.getStartPeriod(), period.getEndPeriod()).toDays() + 1);
            period.setPeriodStatus(periods.isEmpty() ? PeriodStatus.ACTIVE : PeriodStatus.INACTIVE);
            period.setPeriodOrder(periods.size() + 1);
            period.setIsAnnual(request.getIsAnnual());

            periods.add(period);

            startDate = endPeriod.plusDays(1).atStartOfDay();
        }

        return periods;
    }

    public AccountingPeriodResponse updateAccountingPeriod(Long id, AccountingPeriodRequest accountingPeriodRequest) {
        logger.info("Updating accounting period with ID: {}", id);

        String tenantId = getTenantId();

        AccountingPeriodEntity existingAccountingPeriod = accountingPeriodRepository.findById(id)
                .filter(period -> period.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No accounting period found with ID: %d", id)));

        if (isAccountingPeriodExists(accountingPeriodRequest, existingAccountingPeriod.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is already an equal accounting period.");
        }

        if (accountingPeriodRequest.getPeriodStatus() == PeriodStatus.ACTIVE) {
            List<AccountingPeriodEntity> activePeriods = accountingPeriodRepository.findActivePeriods(tenantId);
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
        existingAccountingPeriod.setEndPeriod(accountingPeriodRequest.getEndPeriod());
        existingAccountingPeriod.setDaysPeriod(accountingPeriodRequest.getDaysPeriod());
        existingAccountingPeriod.setPeriodStatus(accountingPeriodRequest.getPeriodStatus());
        existingAccountingPeriod.setPeriodOrder(accountingPeriodRequest.getPeriodOrder());
        existingAccountingPeriod.setIsAnnual(accountingPeriodRequest.getIsAnnual());

        accountingPeriodRepository.save(existingAccountingPeriod);

        return toResponse(existingAccountingPeriod);
    }

    public AccountingPeriodResponse deleteAccountingPeriod(Long id) {
        logger.info("Deleting accounting period with ID: {}", id);

        String tenantId = getTenantId();

        AccountingPeriodEntity existingAccountingPeriod = accountingPeriodRepository.findById(id)
                .filter(period -> period.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No accounting period found with ID: %d", id)));

        accountingPeriodRepository.delete(existingAccountingPeriod);
        return toResponse(existingAccountingPeriod);
    }

    public boolean isActivePeriodExists() {
        String tenantId = getTenantId();
        return !accountingPeriodRepository.findActivePeriods(tenantId).isEmpty();
    }

    @Cacheable("activePeriod")
    public AccountingPeriodEntity getActivePeriod() {
        String tenantId = getTenantId();

        return accountingPeriodRepository.findActivePeriods(tenantId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no active accounting period"));
    }

    public List<AccountingPeriodEntity> getClosedPeriods() {
        String tenantId = getTenantId();

        return accountingPeriodRepository.findByPeriodStatus(tenantId);
    }

    public AccountingPeriodResponse getNextPeriodInfo() {
        AccountingPeriodEntity activePeriod = null;
        String tenantId = getTenantId();

        try {
            activePeriod = getActivePeriod();
        } catch (ResponseStatusException e) {
            logger.warn("No active accounting period found: {}", e.getMessage());
        }

        AccountingPeriodEntity annualPeriod = getAnnualPeriod();
        int currentYear = LocalDate.now().getYear();

        AccountingPeriodEntity nextPeriod = accountingPeriodRepository.findByClosureTypeAndPeriodOrderForYear(
                activePeriod != null ? activePeriod.getClosureType() : annualPeriod.getClosureType(),
                activePeriod != null ? activePeriod.getPeriodOrder() + 1 : 1,
                currentYear,
                tenantId
        );

        if (nextPeriod != null) {
            return toResponse(nextPeriod);
        }

        // Calcular el siguiente período si no existe
        LocalDateTime startPeriod = null;
        LocalDateTime endPeriod = null;
        LocalDate startOfNextYear = annualPeriod.getEndPeriod().toLocalDate().with(TemporalAdjusters.firstDayOfNextYear());

        switch (annualPeriod.getClosureType().toLowerCase()) {
            case CLOSURE_TYPE_MENSUAL:
                startPeriod = startOfNextYear.atStartOfDay();
                endPeriod = startOfNextYear.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
                break;
            case CLOSURE_TYPE_TRIMESTRAL:
                startPeriod = startOfNextYear.atStartOfDay();
                endPeriod = startOfNextYear.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
                break;
            case CLOSURE_TYPE_SEMESTRAL:
                startPeriod = startOfNextYear.atStartOfDay();
                endPeriod = startOfNextYear.plusMonths(5).with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
                break;
            case CLOSURE_TYPE_ANUAL:
                startPeriod = startOfNextYear.atStartOfDay();
                endPeriod = startOfNextYear.with(TemporalAdjusters.lastDayOfYear()).atTime(23, 59, 59);
                break;
            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No next period found and cannot calculate for unsupported closure type: " + annualPeriod.getClosureType()
                );
        }

        AccountingPeriodResponse response = new AccountingPeriodResponse();
        response.setPeriodName(String.format("Periodo %s %d", annualPeriod.getClosureType(), 1));
        response.setClosureType(annualPeriod.getClosureType());
        response.setStartPeriod(startPeriod);
        response.setEndPeriod(endPeriod);
        response.setDaysPeriod((int) Duration.between(startPeriod, endPeriod).toDays() + 1);
        response.setPeriodStatus(PeriodStatus.INACTIVE.toString());
        response.setPeriodOrder(1);
        response.setIsAnnual(annualPeriod.getIsAnnual());

        return response;
    }

    private boolean isAccountingPeriodExists(AccountingPeriodRequest request) {
        String tenantId = getTenantId();

        return accountingPeriodRepository.existsByClosureTypeAndStartPeriodAndTenantId(
                request.getClosureType(),
                request.getStartPeriod(),
                tenantId
        );
    }

    private boolean isAccountingPeriodExists(AccountingPeriodRequest request, Long excludeId) {
        String tenantId = getTenantId();

        return accountingPeriodRepository.existsByClosureTypeAndStartPeriodAndIdNotAndTenantId(
                request.getClosureType(),
                request.getStartPeriod(),
                excludeId,
                tenantId
        );
    }

    public AccountingPeriodEntity getAnnualPeriod() {
        String tenantId = getTenantId();

        return accountingPeriodRepository.findAll().stream()
                .filter(period -> period.getTenantId().equals(tenantId))
                .filter(period -> period.getIsAnnual() != null && period.getIsAnnual())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no annual period available"));
    }

    public AccountingPeriodResponse toResponse(AccountingPeriodEntity entity) {
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
