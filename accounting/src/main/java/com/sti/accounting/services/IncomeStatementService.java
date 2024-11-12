package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.ControlAccountBalancesEntity;
import com.sti.accounting.models.IncomeStatementResponse;
import com.sti.accounting.repositories.IAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class IncomeStatementService {

    private static final Logger logger = LoggerFactory.getLogger(IncomeStatementService.class);

    private final IAccountRepository accountRepository;
    private final ControlAccountBalancesService controlAccountBalancesService;

    public IncomeStatementService(IAccountRepository accountRepository, ControlAccountBalancesService controlAccountBalancesService) {
        this.accountRepository = accountRepository;
        this.controlAccountBalancesService = controlAccountBalancesService;
    }

    public List<IncomeStatementResponse> getIncomeStatement(Long periodId) {
        logger.info("Generating income statement");

        List<AccountEntity> accounts = accountRepository.findAll();
        accounts = accounts.stream()
                .filter(account -> account.getAccountCategory().getName().equalsIgnoreCase("Estado de Resultados"))
                .toList();

        List<IncomeStatementResponse> transactions = new ArrayList<>();

        for (AccountEntity account : accounts) {
            ControlAccountBalancesEntity sumViewEntity;

            if (periodId != null) {
                sumViewEntity = controlAccountBalancesService.getControlAccountBalancesForPeriod(account.getId(), periodId);
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
            totalDebit = totalDebit.add(new BigDecimal(balance.getDebit() != null ? balance.getDebit() : "0"));
            totalCredit = totalCredit.add(new BigDecimal(balance.getCredit() != null ? balance.getCredit() : "0"));
        }

        combined.setDebit(totalDebit.toString());
        combined.setCredit(totalCredit.toString());
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
        BigDecimal debit = sumViewEntity.getDebit() != null ? new BigDecimal(sumViewEntity.getDebit()) : BigDecimal.ZERO;
        BigDecimal credit = sumViewEntity.getCredit() != null ? new BigDecimal(sumViewEntity.getCredit()) : BigDecimal.ZERO;
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