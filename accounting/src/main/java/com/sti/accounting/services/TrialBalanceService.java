package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.entities.ControlAccountBalancesEntity;
import com.sti.accounting.models.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TrialBalanceService {

    private final AccountingPeriodService accountingPeriodService;
    private final ControlAccountBalancesService controlAccountBalancesService;
    private final AccountService accountService;

    public TrialBalanceService(
            AccountingPeriodService accountingPeriodService,
            ControlAccountBalancesService controlAccountBalancesService,
            AccountService accountService) {
        this.accountingPeriodService = accountingPeriodService;
        this.controlAccountBalancesService = controlAccountBalancesService;
        this.accountService = accountService;
    }

    public TrialBalanceResponse getTrialBalance() {
        // Obtener el periodo contable activo
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();
        TrialBalanceResponse trialBalanceResponse = new TrialBalanceResponse();
        List<TrialBalanceResponse.PeriodBalanceResponse> periodBalances = new ArrayList<>();

        List<AccountResponse> allAccounts = accountService.getAllAccount();

        // Verificar si el periodo activo es anual
        if (Boolean.TRUE.equals(activePeriod.getIsAnnual())) {
            // Iterar sobre cada mes del periodo anual
            LocalDateTime startDate = activePeriod.getStartPeriod();
            LocalDateTime endDate = activePeriod.getEndPeriod();

            // Iterar mes a mes
            LocalDateTime currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                AccountingPeriodResponse monthlyPeriod = createMonthlyPeriodResponse(currentDate);
                TrialBalanceResponse.PeriodBalanceResponse periodBalanceResponse = createPeriodBalanceResponse(monthlyPeriod);
                List<TrialBalanceResponse.AccountBalance> accountBalances = new ArrayList<>();

                for (AccountResponse account : allAccounts) {
                    if (isSupportEntry(account)) {
                        TrialBalanceResponse.AccountBalance accountBalance = createAccountBalance(account);
                        TrialBalanceResponse.InitialBalance initialBalanceResponse = calculateInitialBalance(account);
                        accountBalance.setInitialBalance(Collections.singletonList(initialBalanceResponse));

                        TrialBalanceResponse.BalancePeriod balancePeriodResponse = calculateBalancePeriod(account, monthlyPeriod);
                        accountBalance.setBalancePeriod(Collections.singletonList(balancePeriodResponse));

                        TrialBalanceResponse.FinalBalance finalBalanceResponse = calculateFinalBalance(balancePeriodResponse, initialBalanceResponse);
                        accountBalance.setFinalBalance(Collections.singletonList(finalBalanceResponse));

                        accountBalances.add(accountBalance);
                    }
                }

                periodBalanceResponse.setAccountBalances(accountBalances);
                periodBalances.add(periodBalanceResponse);

                // Avanzar al siguiente mes
                currentDate = currentDate.plusMonths(1);
            }
        } else {
            // Si no es anual, continuar con la lógica original
            List<AccountingPeriodResponse> accountingPeriods = accountingPeriodService.getAllAccountingPeriod();
            for (AccountingPeriodResponse period : accountingPeriods) {
                TrialBalanceResponse.PeriodBalanceResponse periodBalanceResponse = createPeriodBalanceResponse(period);
                List<TrialBalanceResponse.AccountBalance> accountBalances = new ArrayList<>();

                for (AccountResponse account : allAccounts) {
                    if (isSupportEntry(account)) {
                        TrialBalanceResponse.AccountBalance accountBalance = createAccountBalance(account);
                        TrialBalanceResponse.InitialBalance initialBalanceResponse = calculateInitialBalance(account);
                        accountBalance.setInitialBalance(Collections.singletonList(initialBalanceResponse));

                        TrialBalanceResponse.BalancePeriod balancePeriodResponse = calculateBalancePeriod(account, period);
                        accountBalance.setBalancePeriod(Collections.singletonList(balancePeriodResponse));

                        TrialBalanceResponse.FinalBalance finalBalanceResponse = calculateFinalBalance(balancePeriodResponse, initialBalanceResponse);
                        accountBalance.setFinalBalance(Collections.singletonList(finalBalanceResponse));

                        accountBalances.add(accountBalance);
                    }
                }

                periodBalanceResponse.setAccountBalances(accountBalances);
                periodBalances.add(periodBalanceResponse);
            }
        }

        trialBalanceResponse.setPeriods(periodBalances);
        return trialBalanceResponse;
    }

    private AccountingPeriodResponse createMonthlyPeriodResponse(LocalDateTime date) {
        AccountingPeriodResponse response = new AccountingPeriodResponse();
        response.setStartPeriod(date);
        response.setEndPeriod(date.plusMonths(1).minusDays(1));
        response.setPeriodName(date.getMonth().name() + " " + date.getYear());
        response.setClosureType("Mensual");
        response.setIsAnnual(false);
        response.setStatus(true);
        return response;
    }

    private TrialBalanceResponse.PeriodBalanceResponse createPeriodBalanceResponse(AccountingPeriodResponse period) {
        TrialBalanceResponse.PeriodBalanceResponse response = new TrialBalanceResponse.PeriodBalanceResponse();
        response.setId(period.getId());
        response.setPeriodName(period.getPeriodName());
        response.setClosureType(period.getClosureType());
        response.setStartPeriod(period.getStartPeriod());
        response.setEndPeriod(period.getEndPeriod());
        response.setStatus(period.isStatus());
        return response;
    }

    private boolean isSupportEntry(AccountResponse account) {
        return account.getSupportEntry() != null && account.getSupportEntry();
    }

    private TrialBalanceResponse.AccountBalance createAccountBalance(AccountResponse account) {
        TrialBalanceResponse.AccountBalance accountBalance = new TrialBalanceResponse.AccountBalance();
        accountBalance.setId(account.getId());
        accountBalance.setName(account.getName());
        accountBalance.setAccountCode(account.getAccountCode());
        accountBalance.setParentName(account.getParentName());
        accountBalance.setParentId(account.getParentId());
        return accountBalance;
    }

    private TrialBalanceResponse.InitialBalance calculateInitialBalance(AccountResponse account) {
        TrialBalanceResponse.InitialBalance initialBalanceResponse = new TrialBalanceResponse.InitialBalance();
        BigDecimal initialBalance = BigDecimal.ZERO;

        if (account.getBalances() != null && !account.getBalances().isEmpty()) {
            Optional<AccountBalance> currentBalanceOpt = account.getBalances().stream()
                    .filter(balance -> Boolean.TRUE.equals(balance.getIsCurrent()))
                    .findFirst();

            if (currentBalanceOpt.isPresent()) {
                AccountBalance currentBalance = currentBalanceOpt.get();
                initialBalance = currentBalance.getInitialBalance() != null ? currentBalance.getInitialBalance() : BigDecimal.ZERO;

                String typicalBalance = currentBalance.getTypicalBalance();

                if ("D".equalsIgnoreCase(typicalBalance)) {
                    initialBalanceResponse.setDebit(initialBalance);
                    initialBalanceResponse.setCredit(BigDecimal.ZERO);
                } else {
                    initialBalanceResponse.setDebit(BigDecimal.ZERO);
                    initialBalanceResponse.setCredit(initialBalance);
                }
            }
        }

        if (initialBalance.equals(BigDecimal.ZERO)) {
            initialBalanceResponse.setDebit(BigDecimal.ZERO);
            initialBalanceResponse.setCredit(BigDecimal.ZERO);
        }

        return initialBalanceResponse;
    }

    private TrialBalanceResponse.BalancePeriod calculateBalancePeriod(AccountResponse account, AccountingPeriodResponse period) {
        TrialBalanceResponse.BalancePeriod balancePeriodResponse = new TrialBalanceResponse.BalancePeriod();

        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        // Obtener el inicio y el final del periodo mensual
        LocalDate startPeriod = period.getStartPeriod().toLocalDate();
        LocalDate endPeriod = period.getEndPeriod().toLocalDate();

        // Inicializar los totales de débito y crédito
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        // Obtener los balances mensuales desde el servicio
        List<ControlAccountBalancesEntity> monthlyBalances = controlAccountBalancesService.getControlAccountBalancesForPeriodAndMonth(account.getId(),activePeriod.getId(), startPeriod, endPeriod);

        for (ControlAccountBalancesEntity balance : monthlyBalances) {
            totalDebit = totalDebit.add(balance.getDebit() != null ? new BigDecimal(balance.getDebit()) : BigDecimal.ZERO);
            totalCredit = totalCredit.add(balance.getCredit() != null ? new BigDecimal(balance.getCredit()) : BigDecimal.ZERO);
        }

        balancePeriodResponse.setDebit(totalDebit);
        balancePeriodResponse.setCredit(totalCredit);

        return balancePeriodResponse;
    }

    private TrialBalanceResponse.FinalBalance calculateFinalBalance(TrialBalanceResponse.BalancePeriod balancePeriod, TrialBalanceResponse.InitialBalance initialBalance) {
        TrialBalanceResponse.FinalBalance finalBalance = new TrialBalanceResponse.FinalBalance();

        BigDecimal totalDebit = initialBalance.getDebit().add(balancePeriod.getDebit());
        BigDecimal totalCredit = initialBalance.getCredit().add(balancePeriod.getCredit());

        if (totalDebit.compareTo(totalCredit) > 0) {
            finalBalance.setDebit(totalDebit.subtract(totalCredit));
            finalBalance.setCredit(BigDecimal.ZERO);
        } else {
            finalBalance.setDebit(BigDecimal.ZERO);
            finalBalance.setCredit(totalCredit.subtract(totalDebit));
        }

        return finalBalance;
    }
}