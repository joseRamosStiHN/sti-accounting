package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingClosingEntity;
import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.entities.ControlAccountBalancesEntity;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountingClosingRepository;
import com.sti.accounting.repositories.IAccountingPeriodRepository;
import com.sti.accounting.repositories.IBalancesRepository;
import com.sti.accounting.repositories.IControlAccountBalancesRepository;
import com.sti.accounting.utils.PeriodStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountingClosingService {

    private static final Logger logger = LoggerFactory.getLogger(AccountingClosingService.class);

    private final IAccountingClosingRepository accountingClosingRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final GeneralBalanceService generalBalanceService;
    private final IncomeStatementService incomeStatementService;
    private final IAccountingPeriodRepository accountingPeriodRepository;
    private final BalancesService balancesService;
    private final IBalancesRepository iBalancesRepository;
    private final IControlAccountBalancesRepository controlAccountBalancesRepository;

    public AccountingClosingService(IAccountingClosingRepository accountingClosingRepository, AccountingPeriodService accountingPeriodService, GeneralBalanceService generalBalanceService, IncomeStatementService incomeStatementService, IAccountingPeriodRepository accountingPeriodRepository, BalancesService balancesService, IBalancesRepository iBalancesRepository, IControlAccountBalancesRepository controlAccountBalancesRepository) {
        this.accountingClosingRepository = accountingClosingRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.generalBalanceService = generalBalanceService;
        this.incomeStatementService = incomeStatementService;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.balancesService = balancesService;
        this.iBalancesRepository = iBalancesRepository;
        this.controlAccountBalancesRepository = controlAccountBalancesRepository;
    }

    public List<AccountingClosingResponse> getAllAccountingClosing() {
        return this.accountingClosingRepository.findAll().stream().map(this::toResponse).toList();
    }


    public AccountingClosingResponse getDetailAccountingClosing() {
        logger.info("Generating detail accounting closing");

        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        AccountingClosingResponse accountingClosingResponse = new AccountingClosingResponse();

        accountingClosingResponse.setPeriodName(activePeriod.getPeriodName());
        accountingClosingResponse.setTypePeriod(activePeriod.getClosureType());
        accountingClosingResponse.setStartPeriod(activePeriod.getStartPeriod());
        accountingClosingResponse.setEndPeriod(activePeriod.getEndPeriod());

        // Obtener el balance general
        List<GeneralBalanceResponse> balanceResponses = generalBalanceService.getBalanceGeneral(activePeriod.getId());

        // Calcular totales
        BigDecimal totalAssets = balanceResponses.stream()
                .filter(item -> "ACTIVO".equals(item.getCategory()))
                .map(GeneralBalanceResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiabilities = balanceResponses.stream()
                .filter(item -> "PASIVO".equals(item.getCategory()))
                .map(GeneralBalanceResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCapital = balanceResponses.stream()
                .filter(item -> "PATRIMONIO".equals(item.getCategory()))
                .map(GeneralBalanceResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Asignar totales al response
        accountingClosingResponse.setTotalAssets(totalAssets);
        accountingClosingResponse.setTotalLiabilities(totalLiabilities);
        accountingClosingResponse.setTotalCapital(totalCapital);

        // Obtener el estado de resultados
        List<IncomeStatementResponse> incomeStatementResponses = incomeStatementService.getIncomeStatement(activePeriod.getId());

        // Calcular totales de ingresos y gastos
        BigDecimal totalIncome = incomeStatementResponses.stream()
                .filter(item -> "C".equals(item.getTypicalBalance()))
                .map(IncomeStatementResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = incomeStatementResponses.stream()
                .filter(item -> "D".equals(item.getTypicalBalance()))
                .map(IncomeStatementResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Asignar totales de ingresos y gastos al response
        accountingClosingResponse.setTotalIncome(totalIncome);
        accountingClosingResponse.setTotalExpenses(totalExpenses);

        // Calcular el ingreso neto
        BigDecimal netIncome = incomeStatementService.getNetProfit(incomeStatementResponses);
        accountingClosingResponse.setNetIncome(netIncome);

        return accountingClosingResponse;
    }

    public void closeAccountingPeriod(String newClosureType) {
        logger.info("Closing accounting period with new closure type: {}", newClosureType);

        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();
        logger.info("Active accounting period ID: {}", activePeriod.getId());

        // Process balances for the active accounting period
        processBalances(activePeriod);

        // Save the accounting closing record
        saveAccountingClosing(activePeriod);

        // Close the active accounting period
        closeActivePeriod(activePeriod);

        // Activate or create the next accounting period
        activateOrCreateNextPeriod(activePeriod, newClosureType);
    }


    private void activateOrCreateNextPeriod(AccountingPeriodEntity currentPeriod, String newClosureType) {
        List<AccountingPeriodEntity> nextPeriods = accountingPeriodRepository.findNextPeriod(currentPeriod.getEndPeriod(), newClosureType);

        // Verifica si hay más de un período siguiente
        if (nextPeriods.size() > 1) {
            logger.warn("Se encontraron múltiples períodos siguientes. Activando el primero: {}", nextPeriods.get(0).getId());
        }

        // Toma el primer período siguiente si existe
        if (!nextPeriods.isEmpty()) {
            AccountingPeriodEntity nextPeriod = nextPeriods.get(0);
            nextPeriod.setPeriodStatus(PeriodStatus.ACTIVE);
            accountingPeriodRepository.save(nextPeriod);
            logger.info("El período contable ID {} ha sido activado.", nextPeriod.getId());
        } else {
            logger.warn("No se encontró ningún período siguiente. Creando nuevos períodos contables.");
            createMissingAccountingPeriods(currentPeriod.getEndPeriod(), newClosureType);
        }
    }

    private void createMissingAccountingPeriods(LocalDateTime startPeriod, String closureType) {
        int currentMonth = startPeriod.getMonthValue();
        int remainingMonths = 12 - currentMonth;
        int periodsToCreate;

        switch (closureType.toLowerCase()) {
            case "mensual":
                periodsToCreate = remainingMonths;
                break;
            case "trimestral":
                if (remainingMonths < 3) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            String.format("No hay suficientes meses restantes en el año contable para crear un período trimestral. Quedan %d meses.", remainingMonths));
                }
                periodsToCreate = 3;
                break;
            case "semestral":
                if (remainingMonths < 6) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            String.format("No hay suficientes meses restantes en el año contable para crear un período semestral. Quedan %d meses.", remainingMonths));
                }
                periodsToCreate = 6;
                break;
            default:
                logger.warn("Closure type not supported for period creation: {}", closureType);
                return;
        }

        createAndSavePeriods(startPeriod, closureType, currentMonth, periodsToCreate);
    }

    private void createAndSavePeriods(LocalDateTime startPeriod, String closureType, int currentMonth, int periodsToCreate) {
        for (int i = 1; i <= periodsToCreate; i++) {
            LocalDateTime newStartPeriod = startPeriod.plusMonths(i).withDayOfMonth(1);
            LocalDateTime newEndPeriod = newStartPeriod.withDayOfMonth(newStartPeriod.toLocalDate().lengthOfMonth());

            AccountingPeriodEntity newPeriod = new AccountingPeriodEntity();
            newPeriod.setStartPeriod(newStartPeriod);
            newPeriod.setEndPeriod(newEndPeriod);
            newPeriod.setClosureType(closureType);
            newPeriod.setPeriodName(String.format("%s %d", closureType, (currentMonth + i)));
            newPeriod.setDaysPeriod((int) java.time.Duration.between(newStartPeriod, newEndPeriod).toDays() + 1);
            newPeriod.setPeriodStatus(i == 1 ? PeriodStatus.ACTIVE : PeriodStatus.INACTIVE);
            newPeriod.setPeriodOrder(i + 1);
            newPeriod.setIsAnnual(false);
            accountingPeriodRepository.save(newPeriod);
        }
    }

    private void processBalances(AccountingPeriodEntity activePeriod) {
        List<ControlAccountBalancesEntity> accountBalances = controlAccountBalancesRepository.findAllByAccountingPeriodId(activePeriod.getId());
        if (accountBalances.isEmpty()) {
            logger.warn("No account balances found for the active accounting period.");
            return;
        }

        Map<Long, List<ControlAccountBalancesEntity>> groupedBalances = accountBalances.stream()
                .collect(Collectors.groupingBy(ControlAccountBalancesEntity::getAccountId));

        for (Map.Entry<Long, List<ControlAccountBalancesEntity>> entry : groupedBalances.entrySet()) {
            Long accountId = entry.getKey();
            List<ControlAccountBalancesEntity> balancesForAccount = entry.getValue();
            logger.info("Processing account balance for account ID: {}", accountId);

            inactivateExistingBalances(accountId);
            createNewBalance(balancesForAccount);
        }
    }

    private void saveAccountingClosing(AccountingPeriodEntity activePeriod) {
        AccountingClosingResponse closingDetails = getDetailAccountingClosing();

        AccountingClosingEntity closingEntity = new AccountingClosingEntity();
        closingEntity.setAccountingPeriod(activePeriod);
        closingEntity.setStartPeriod(closingDetails.getStartPeriod());
        closingEntity.setEndPeriod(closingDetails.getEndPeriod());
        closingEntity.setTotalAssets(closingDetails.getTotalAssets());
        closingEntity.setTotalLiabilities(closingDetails.getTotalLiabilities());
        closingEntity.setTotalCapital(closingDetails.getTotalCapital());
        closingEntity.setTotalIncome(closingDetails.getTotalIncome());
        closingEntity.setTotalExpenses(closingDetails.getTotalExpenses());
        closingEntity.setNetIncome(closingDetails.getNetIncome());

        accountingClosingRepository.save(closingEntity);
        logger.info("Accounting closing saved for period ID: {}", activePeriod.getId());
    }

    private void closeActivePeriod(AccountingPeriodEntity activePeriod) {
        activePeriod.setPeriodStatus(PeriodStatus.CLOSED);
        accountingPeriodRepository.save(activePeriod);
    }

    private void inactivateExistingBalances(Long accountId) {
        List<BalancesEntity> existingBalances = iBalancesRepository.findByAccountId(accountId);
        for (BalancesEntity existingBalance : existingBalances) {
            existingBalance.setIsCurrent(false);
            iBalancesRepository.save(existingBalance);
        }
    }

    private void createNewBalance(List<ControlAccountBalancesEntity> accountBalances) {
        BalancesRequest balancesRequest = new BalancesRequest();

        if (!accountBalances.isEmpty()) {
            Long accountId = accountBalances.get(0).getAccountId();
            balancesRequest.setAccountId(accountId);

            BalancesEntity balancesEntity = iBalancesRepository.findMostRecentBalanceByAccountId(accountId);

            BigDecimal totalDebit = accountBalances.stream()
                    .map(ControlAccountBalancesEntity::getDebit)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCredit = accountBalances.stream()
                    .map(ControlAccountBalancesEntity::getCredit)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (balancesEntity != null) {
                if ("D".equals(balancesEntity.getTypicalBalance())) {
                    totalDebit = totalDebit.add(balancesEntity.getInitialBalance());
                } else if ("C".equals(balancesEntity.getTypicalBalance())) {
                    totalCredit = totalCredit.add(balancesEntity.getInitialBalance());
                }
            }

            balancesRequest.setTypicalBalance(totalDebit.compareTo(totalCredit) > 0 ? "D" : "C");
            balancesRequest.setInitialBalance(totalDebit.subtract(totalCredit));
        }

        balancesRequest.setIsCurrent(true);
        balancesService.createBalance(balancesRequest);
    }

    private AccountingClosingResponse toResponse(AccountingClosingEntity accountingClosingEntity) {
        AccountingClosingResponse accountingClosingResponse = new AccountingClosingResponse();

        accountingClosingResponse.setAccountingPeriodId(accountingClosingEntity.getAccountingPeriod().getId());
        accountingClosingResponse.setStartPeriod(accountingClosingEntity.getStartPeriod());
        accountingClosingResponse.setEndPeriod(accountingClosingEntity.getEndPeriod());
        accountingClosingResponse.setTotalAssets(accountingClosingEntity.getTotalAssets());
        accountingClosingResponse.setTotalLiabilities(accountingClosingEntity.getTotalLiabilities());
        accountingClosingResponse.setTotalCapital(accountingClosingEntity.getTotalCapital());
        accountingClosingResponse.setTotalIncome(accountingClosingEntity.getTotalIncome());
        accountingClosingResponse.setTotalExpenses(accountingClosingEntity.getTotalExpenses());
        accountingClosingResponse.setNetIncome(accountingClosingEntity.getNetIncome());
        return accountingClosingResponse;

    }
}