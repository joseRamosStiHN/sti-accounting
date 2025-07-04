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

    public TrialBalanceService(AccountingPeriodService accountingPeriodService,
                               ControlAccountBalancesService controlAccountBalancesService,
                               AccountService accountService) {
        this.accountingPeriodService = accountingPeriodService;
        this.controlAccountBalancesService = controlAccountBalancesService;
        this.accountService = accountService;
    }

    //ToDo: Revisar logica para obtener la balanza de comprobacion total todos los meses hasta el cierre anual
    public TrialBalanceResponse getTrialBalance() {
        TrialBalanceResponse trialBalanceResponse = new TrialBalanceResponse();
        List<TrialBalanceResponse.PeriodBalanceResponse> periodBalances = new ArrayList<>();
        List<AccountResponse> allAccounts = accountService.getAllAccount();

        // Obtener el período activo
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();
        if (activePeriod != null && activePeriod.getStartPeriod() != null) {
            // Crear la respuesta del balance del período activo
            TrialBalanceResponse.PeriodBalanceResponse activePeriodBalanceResponse = createPeriodBalanceResponse(activePeriod, allAccounts, false);
            periodBalances.add(activePeriodBalanceResponse);

            // Obtener el año del período activo
            int activeYear = activePeriod.getStartPeriod().getYear();

            // Obtener los períodos cerrados y filtrar por el año activo
            List<AccountingPeriodEntity> closedPeriods = accountingPeriodService.getClosedPeriods();
            for (AccountingPeriodEntity closedPeriod : closedPeriods) {
                if (closedPeriod.getStartPeriod() != null
                        && closedPeriod.getStartPeriod().getYear() == activeYear
                        && Boolean.TRUE.equals(!closedPeriod.getIsAnnual())) {
                    TrialBalanceResponse.PeriodBalanceResponse closedPeriodBalanceResponse = createPeriodBalanceResponse(closedPeriod, allAccounts, false);
                    periodBalances.add(closedPeriodBalanceResponse);
                }
            }
        }

        trialBalanceResponse.setPeriods(periodBalances);
        return trialBalanceResponse;
    }

    public TrialBalanceResponse getTrialBalancePdf() {
        TrialBalanceResponse trialBalanceResponse = new TrialBalanceResponse();
        List<TrialBalanceResponse.PeriodBalanceResponse> periodBalances = new ArrayList<>();
        List<AccountResponse> allAccounts = accountService.getAllAccount();

        // Obtener el período activo
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();
        if (activePeriod != null && activePeriod.getStartPeriod() != null) {
            // Crear la respuesta del balance del período activo
            TrialBalanceResponse.PeriodBalanceResponse activePeriodBalanceResponse = createPeriodBalanceResponse(activePeriod, allAccounts, false);
            periodBalances.add(activePeriodBalanceResponse);
        }

        trialBalanceResponse.setPeriods(periodBalances);
        return trialBalanceResponse;
    }


    public TrialBalanceResponse getAllTrialBalances() {
        TrialBalanceResponse trialBalanceResponse = new TrialBalanceResponse();
        List<TrialBalanceResponse.PeriodBalanceResponse> periodBalances = new ArrayList<>();
        List<AccountResponse> allAccounts = accountService.getAllAccount().stream().filter(f -> f.getSupportEntry() != null  ).toList();

        // Obtener todos los períodos contables
        List<AccountingPeriodResponse> allAccountingPeriods = accountingPeriodService.getAllAccountingPeriod();

        for (AccountingPeriodResponse periodResponse : allAccountingPeriods) {
            if (periodResponse.getStartPeriod() != null) {
                // Convertir AccountingPeriodResponse a AccountingPeriodEntity
                AccountingPeriodEntity period = new AccountingPeriodEntity();
                period.setId(periodResponse.getId());
                period.setPeriodName(periodResponse.getPeriodName());
                period.setStartPeriod(periodResponse.getStartPeriod());
                period.setEndPeriod(periodResponse.getEndPeriod());

                TrialBalanceResponse.PeriodBalanceResponse periodBalanceResponse = createPeriodBalanceResponse(period, allAccounts, true);
                periodBalances.add(periodBalanceResponse);
            }
        }

        trialBalanceResponse.setPeriods(periodBalances);
        return trialBalanceResponse;
    }

    private TrialBalanceResponse.PeriodBalanceResponse createPeriodBalanceResponse(AccountingPeriodEntity period, List<AccountResponse> allAccounts, boolean useFirstBalance) {
        TrialBalanceResponse.PeriodBalanceResponse periodBalanceResponse = new TrialBalanceResponse.PeriodBalanceResponse();
        periodBalanceResponse.setPeriodName(period.getPeriodName());
        periodBalanceResponse.setStartPeriod(period.getStartPeriod());
        periodBalanceResponse.setEndPeriod(period.getEndPeriod());

        List<TrialBalanceResponse.AccountBalance> accountBalances = new ArrayList<>();
        Map<Long, TrialBalanceResponse.FinalBalance> finalBalancesMap = new HashMap<>();

        for (AccountResponse account : allAccounts) {
                TrialBalanceResponse.AccountBalance accountBalance = createAccountBalance(account);

                // Calcular el balance inicial
                TrialBalanceResponse.InitialBalance initialBalanceResponse;
                if (useFirstBalance) {
                    initialBalanceResponse = calculateInitialBalanceUsingOldest(account);
                } else {
                    initialBalanceResponse = calculateInitialBalance(account);
                }
                accountBalance.setInitialBalance(Collections.singletonList(initialBalanceResponse));

                // Calcular el balance para el rango de fechas
                TrialBalanceResponse.BalancePeriod balancePeriodResponse = calculateBalanceForDateRange(account, period.getStartPeriod(), period.getEndPeriod());
                accountBalance.setBalancePeriod(Collections.singletonList(balancePeriodResponse));

                // Calcular el balance final
                TrialBalanceResponse.FinalBalance finalBalanceResponse = calculateFinalBalance(balancePeriodResponse, initialBalanceResponse, account);
                accountBalance.setFinalBalance(Collections.singletonList(finalBalanceResponse));

                // Almacenar el balance final en el mapa
                finalBalancesMap.put(account.getId(), finalBalanceResponse);

                accountBalances.add(accountBalance);
       }

        periodBalanceResponse.setAccountBalances(accountBalances);
        return periodBalanceResponse;
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
                initialBalance = Optional.ofNullable(currentBalance.getInitialBalance()).orElse(BigDecimal.ZERO);
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

    private TrialBalanceResponse.InitialBalance calculateInitialBalanceUsingOldest(AccountResponse account) {
        TrialBalanceResponse.InitialBalance initialBalanceResponse = new TrialBalanceResponse.InitialBalance();
        BigDecimal initialBalance = BigDecimal.ZERO;

        if (account.getBalances() != null && !account.getBalances().isEmpty()) {
            // Convertir el Set a una List y ordenar por createAtDate
            List<AccountBalance> balancesList = new ArrayList<>(account.getBalances());

            // Ordenar los balances por la fecha de creación (createAtDate)
            balancesList.sort(Comparator.comparing(AccountBalance::getCreateAtDate));

            // Tomar el balance más antiguo
            AccountBalance oldestBalance = balancesList.get(0);

            initialBalance = Optional.ofNullable(oldestBalance.getInitialBalance()).orElse(BigDecimal.ZERO);
            String typicalBalance = oldestBalance.getTypicalBalance();

            if ("D".equalsIgnoreCase(typicalBalance)) {
                initialBalanceResponse.setDebit(initialBalance);
                initialBalanceResponse.setCredit(BigDecimal.ZERO);
            } else {
                initialBalanceResponse.setDebit(BigDecimal.ZERO);
                initialBalanceResponse.setCredit(initialBalance);
            }
        }

        if (initialBalance.equals(BigDecimal.ZERO)) {
            initialBalanceResponse.setDebit(BigDecimal.ZERO);
            initialBalanceResponse.setCredit(BigDecimal.ZERO);
        }

        return initialBalanceResponse;
    }

    private TrialBalanceResponse.BalancePeriod calculateBalanceForDateRange(AccountResponse account, LocalDateTime startDate, LocalDateTime endDate) {
        TrialBalanceResponse.BalancePeriod balancePeriodResponse = new TrialBalanceResponse.BalancePeriod();
        LocalDate startLocalDate = startDate.toLocalDate();
        LocalDate endLocalDate = endDate.toLocalDate();

        List<ControlAccountBalancesEntity> balances = controlAccountBalancesService.getControlAccountBalancesForDateRange(account.getId(), startLocalDate, endLocalDate);

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (ControlAccountBalancesEntity balance : balances) {
            totalDebit = totalDebit.add(Optional.ofNullable(balance.getDebit()).orElse(BigDecimal.ZERO));
            totalCredit = totalCredit.add(Optional.ofNullable(balance.getCredit()).orElse(BigDecimal.ZERO));
        }

        balancePeriodResponse.setDebit(totalDebit);
        balancePeriodResponse.setCredit(totalCredit);

        return balancePeriodResponse;
    }

    private TrialBalanceResponse.FinalBalance calculateFinalBalance(
            TrialBalanceResponse.BalancePeriod balancePeriod,
            TrialBalanceResponse.InitialBalance initialBalance,
            AccountResponse account) {

        TrialBalanceResponse.FinalBalance finalBalance = new TrialBalanceResponse.FinalBalance();

        // Determina el tipo de balance típico (debe ser "D" o "C")
        String typicalBalance = account.getBalances().stream()
                .filter(balance -> Boolean.TRUE.equals(balance.getIsCurrent()))
                .findFirst()
                .map(AccountBalance::getTypicalBalance)
                .orElse("D");

        // Suma los débitos y créditos
        BigDecimal totalDebit = initialBalance.getDebit().add(balancePeriod.getDebit());
        BigDecimal totalCredit = initialBalance.getCredit().add(balancePeriod.getCredit());

        // Calcula el balance neto
        BigDecimal netBalance;
        if ("D".equalsIgnoreCase(typicalBalance)) {
            netBalance = totalDebit.subtract(totalCredit);
            finalBalance.setDebit(netBalance.max(BigDecimal.ZERO));
            finalBalance.setCredit(netBalance.min(BigDecimal.ZERO).abs());
        } else {
            netBalance = totalCredit.subtract(totalDebit);
            finalBalance.setCredit(netBalance.max(BigDecimal.ZERO));
            finalBalance.setDebit(netBalance.min(BigDecimal.ZERO).abs());
        }

        return finalBalance;
    }

}
