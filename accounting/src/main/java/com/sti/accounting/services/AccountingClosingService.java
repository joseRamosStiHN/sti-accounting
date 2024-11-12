package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountingPeriodRepository;
import com.sti.accounting.repositories.IBalancesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountingClosingService {

    private static final Logger logger = LoggerFactory.getLogger(AccountingClosingService.class);

    private final AccountingPeriodService accountingPeriodService;
    private final GeneralBalanceService generalBalanceService;
    private final IncomeStatementService incomeStatementService;
    private final IAccountingPeriodRepository accountingPeriodRepository;
    private final BalancesService balancesService;
    private final TrialBalanceService trialBalanceService;
    private final IBalancesRepository iBalancesRepository;

    public AccountingClosingService(AccountingPeriodService accountingPeriodService, GeneralBalanceService generalBalanceService, IncomeStatementService incomeStatementService, IAccountingPeriodRepository accountingPeriodRepository, BalancesService balancesService, TrialBalanceService trialBalanceService, IBalancesRepository iBalancesRepository) {
        this.accountingPeriodService = accountingPeriodService;
        this.generalBalanceService = generalBalanceService;
        this.incomeStatementService = incomeStatementService;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.balancesService = balancesService;
        this.trialBalanceService = trialBalanceService;
        this.iBalancesRepository = iBalancesRepository;
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

    public void closeAccountingPeriod() {
        logger.info("Closing accounting period");

        // Obtener el período contable activo
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        // Obtener el balance de prueba
        TrialBalanceResponse trialBalanceResponse = trialBalanceService.getTrialBalance();

        // Verificar si hay períodos
        if (trialBalanceResponse.getPeriods().isEmpty()) {
            logger.warn("No periods found in TrialBalanceResponse.");
            return;
        }

        // Iterar sobre cada período en el balance de prueba
        for (TrialBalanceResponse.PeriodBalanceResponse periodBalance : trialBalanceResponse.getPeriods()) {
            if (!periodBalance.isStatus()) {
                continue; // Solo procesar períodos activos
            }

            processActivePeriod(periodBalance);
        }

        // Cerrar el período contable
        closeActivePeriod(activePeriod);
    }

    private void processActivePeriod(TrialBalanceResponse.PeriodBalanceResponse periodBalance) {
        for (TrialBalanceResponse.AccountBalance accountBalance : periodBalance.getAccountBalances()) {
            inactivateExistingBalances(accountBalance.getId());

            TrialBalanceResponse.FinalBalance finalBalance = getFinalBalance(accountBalance);
            if (finalBalance == null || isBalanceEmpty(finalBalance)) {
                continue; // Si no hay saldo final o está vacío, continuar
            }

            createNewBalance(accountBalance, finalBalance);
        }
    }

    private void inactivateExistingBalances(Long accountId) {
        List<BalancesEntity> existingBalances = iBalancesRepository.findByAccountId(accountId);
        for (BalancesEntity existingBalance : existingBalances) {
            existingBalance.setIsCurrent(false);
            iBalancesRepository.save(existingBalance);
        }
    }

    private TrialBalanceResponse.FinalBalance getFinalBalance(TrialBalanceResponse.AccountBalance accountBalance) {
        List<TrialBalanceResponse.FinalBalance> finalBalances = accountBalance.getFinalBalance();
        return finalBalances.isEmpty() ? null : finalBalances.get(0);
    }

    private boolean isBalanceEmpty(TrialBalanceResponse.FinalBalance finalBalance) {
        return finalBalance.getDebit().compareTo(BigDecimal.ZERO) == 0 && finalBalance.getCredit().compareTo(BigDecimal.ZERO) == 0;
    }

    private void createNewBalance(TrialBalanceResponse.AccountBalance accountBalance, TrialBalanceResponse.FinalBalance finalBalance) {
        BalancesRequest balancesRequest = new BalancesRequest();
        balancesRequest.setAccountId(accountBalance.getId());

        if (finalBalance.getDebit().compareTo(BigDecimal.ZERO) > 0) {
            balancesRequest.setTypicalBalance("D");
            balancesRequest.setInitialBalance(finalBalance.getDebit());
        } else {
            balancesRequest.setTypicalBalance("C");
            balancesRequest.setInitialBalance(finalBalance.getCredit());
        }

        balancesRequest.setIsCurrent(true);
        balancesService.createBalance(balancesRequest);
    }

    private void closeActivePeriod(AccountingPeriodEntity activePeriod) {
        activePeriod.setStatus(false);
        this.accountingPeriodRepository.save(activePeriod);
    }

}
