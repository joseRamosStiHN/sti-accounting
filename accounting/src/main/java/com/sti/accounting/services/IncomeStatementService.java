package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.ControlAccountBalancesEntity;
import com.sti.accounting.models.AccountingPeriodResponse;
import com.sti.accounting.models.IncomeStatementResponse;
import com.sti.accounting.repositories.IAccountRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class IncomeStatementService {

    private static final Logger logger = LoggerFactory.getLogger(IncomeStatementService.class);

    private final IAccountRepository accountRepository;
    private final ControlAccountBalancesService controlAccountBalancesService;
    private final AccountingPeriodService accountingPeriodService;
    private final AuthService authService;
    public IncomeStatementService(IAccountRepository accountRepository, ControlAccountBalancesService controlAccountBalancesService, AccountingPeriodService accountingPeriodService, AuthService authService) {
        this.accountRepository = accountRepository;
        this.controlAccountBalancesService = controlAccountBalancesService;
        this.accountingPeriodService = accountingPeriodService;
        this.authService = authService;
    }

//    private String getTenantId() {
//        return TenantContext.getCurrentTenant();
//    }

    public List<IncomeStatementResponse> getIncomeStatement(Long periodId) {
        logger.info("Generating income statement");
        String tenantId = authService.getTenantId();

        List<AccountEntity> accounts = accountRepository.findAll().stream().filter(balances -> balances.getTenantId().equals(tenantId)).toList();
        accounts = accounts.stream()
                .filter(account -> account.getAccountCategory().getName().equalsIgnoreCase("Estado de Resultados"))
                .toList();

        List<IncomeStatementResponse> transactions = new ArrayList<>();


        for (AccountEntity account : accounts) {
            ControlAccountBalancesEntity sumViewEntity;

            if (periodId != null) {
                AccountingPeriodResponse period = accountingPeriodService.getById(periodId);
                if (period != null) {
                    // Obtener el inicio y el final del periodo mensual
                    LocalDate startPeriod = period.getStartPeriod().toLocalDate();
                    LocalDate endPeriod = period.getEndPeriod().toLocalDate();

                    List<ControlAccountBalancesEntity> balances = controlAccountBalancesService.getControlAccountBalancesForPeriodAndMonth(account.getId(), period.getId(), startPeriod, endPeriod);
                    sumViewEntity = combineBalances(balances);
                } else {
                    List<ControlAccountBalancesEntity> balances = controlAccountBalancesService.getControlAccountBalancesForAllPeriods(account.getId());
                    sumViewEntity = combineBalances(balances);
                }

            } else {
                List<ControlAccountBalancesEntity> balances = controlAccountBalancesService.getControlAccountBalancesForAllPeriods(account.getId());
                sumViewEntity = combineBalances(balances);
            }

            BigDecimal balance = getBalance(sumViewEntity);
            IncomeStatementResponse transaction = getIncomeStatementResponse(account, balance);
            transactions.add(transaction);
        }

        return transactions;
    }

    private ControlAccountBalancesEntity combineBalances(List<ControlAccountBalancesEntity> balances) {
        ControlAccountBalancesEntity combined = new ControlAccountBalancesEntity();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (ControlAccountBalancesEntity balance : balances) {
            totalDebit = totalDebit.add(balance.getDebit() != null ? balance.getDebit() : BigDecimal.valueOf(0));
            totalCredit = totalCredit.add(balance.getCredit() != null ? balance.getCredit() : BigDecimal.valueOf(0));
        }

        combined.setDebit(totalDebit);
        combined.setCredit(totalCredit);
        return combined;
    }

    private static IncomeStatementResponse getIncomeStatementResponse(AccountEntity account, BigDecimal balance) {
        IncomeStatementResponse transaction = new IncomeStatementResponse();
        transaction.setId(account.getId());
        transaction.setCategory(account.getAccountType() != null ? account.getAccountType().getName() : null);
        transaction.setAccountParent(account.getParent() != null ? account.getParent().getDescription() : null);
        transaction.setTypicalBalance(account.getTypicalBalance());
        transaction.setAccount(account.getDescription());
        transaction.setAmount(balance);
        transaction.setDate(new Date());
        return transaction;
    }

    private BigDecimal getBalance(ControlAccountBalancesEntity sumViewEntity) {
        if (sumViewEntity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal debit = sumViewEntity.getDebit() != null ? sumViewEntity.getDebit() : BigDecimal.ZERO;
        BigDecimal credit = sumViewEntity.getCredit() != null ? sumViewEntity.getCredit() : BigDecimal.ZERO;
        return debit.subtract(credit).abs();
    }

    public BigDecimal getNetProfit(List<IncomeStatementResponse> transactions) {
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalDebit = BigDecimal.ZERO;

        for (IncomeStatementResponse transaction : transactions) {
            if (transaction.getTypicalBalance() != null) {
                if (transaction.getTypicalBalance().equalsIgnoreCase("C")) {
                    totalCredit = totalCredit.add(transaction.getAmount());
                } else if (transaction.getTypicalBalance().equalsIgnoreCase("D")) {
                    totalDebit = totalDebit.add(transaction.getAmount());
                }
            }
        }

        return totalCredit.subtract(totalDebit);
    }
}