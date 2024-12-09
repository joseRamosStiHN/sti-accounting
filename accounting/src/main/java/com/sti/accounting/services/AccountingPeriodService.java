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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
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
        // Inactivar todos los períodos activos antes de crear nuevos
        List<AccountingPeriodEntity> activePeriods = accountingPeriodRepository.findActivePeriods();
        for (AccountingPeriodEntity activePeriod : activePeriods) {
            activePeriod.setPeriodStatus(PeriodStatus.INACTIVE);
            accountingPeriodRepository.save(activePeriod);
        }

        List<AccountingPeriodEntity> periods = generatePeriods(accountingPeriodRequest);

        // Guardar todos los períodos en la base de datos
        accountingPeriodRepository.saveAll(periods);

        // El primer período es el activo
        AccountingPeriodEntity activePeriod = periods.get(0);
        activePeriod.setPeriodStatus(PeriodStatus.ACTIVE);
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
                case "mensual":
                    // Fin del mes actual
                    endPeriod = startDate.toLocalDate().with(TemporalAdjusters.lastDayOfMonth());
                    break;

                case "trimestral":
                    // Calcular el fin del trimestre como 3 meses desde el inicio, pero limitado al fin del año
                    endPeriod = startDate.toLocalDate().plusMonths(3).withDayOfMonth(1).minusDays(1);
                    if (endPeriod.isAfter(endOfYear)) {
                        endPeriod = endOfYear;
                    }
                    break;

                case "semestral":
                    // Calcular el fin del semestre como 6 meses desde el inicio, pero limitado al fin del año
                    endPeriod = startDate.toLocalDate().plusMonths(6).withDayOfMonth(1).minusDays(1);
                    if (endPeriod.isAfter(endOfYear)) {
                        endPeriod = endOfYear;
                    }
                    break;


                case "anual":
                    // Fin del año
                    endPeriod = endOfYear;
                    break;

                default:
                    throw new IllegalArgumentException("Tipo de cierre no soportado: " + request.getClosureType());
            }

            period.setEndPeriod(endPeriod.atTime(23, 59, 59));
            period.setDaysPeriod((int) java.time.Duration.between(period.getStartPeriod(), period.getEndPeriod()).toDays() + 1);
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

    public AccountingPeriodResponse getNextPeriodInfo() {
        AccountingPeriodEntity activePeriod = getActivePeriod();

        AccountingPeriodEntity nextPeriod = accountingPeriodRepository.findByClosureTypeAndPeriodOrder(activePeriod.getClosureType(), activePeriod.getPeriodOrder() + 1);

        if (nextPeriod != null) {
            return toResponse(nextPeriod);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No next period found to retrieve information.");
        }
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
