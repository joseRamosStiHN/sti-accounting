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

    public List<IncomeStatementResponse> getIncomeStatement() {
        logger.info("Generating income statement");

        List<AccountEntity> accounts = accountRepository.findAll();

        accounts = accounts.stream()
                .filter(account -> account.getAccountCategory().getName().equalsIgnoreCase("Estado de Resultados"))
                .toList();

        List<IncomeStatementResponse> transactions = new ArrayList<>();

        for (AccountEntity account : accounts) {
            ControlAccountBalancesEntity sumViewEntity = controlAccountBalancesService.getControlAccountBalances(account.getId());
            BigDecimal balance = getBalance(sumViewEntity);

            IncomeStatementResponse transaction = new IncomeStatementResponse();
            transaction.setId(account.getId());
            transaction.setCategory(account.getAccountType() != null ? account.getAccountType().getName() : null);
            transaction.setAccountParent(account.getParent() != null ? account.getParent().getDescription() : null);
            transaction.setAccount(account.getDescription());
            transaction.setAmount(balance);
            transaction.setDate(new Date());

            transactions.add(transaction);
        }

        return transactions;
    }

    private BigDecimal getBalance(ControlAccountBalancesEntity sumViewEntity) {
        BigDecimal debit = sumViewEntity.getDebit() != null ? new BigDecimal(sumViewEntity.getDebit()) : BigDecimal.ZERO;
        BigDecimal credit = sumViewEntity.getCredit() != null ? new BigDecimal(sumViewEntity.getCredit()) : BigDecimal.ZERO;
        return debit.subtract(credit).abs();
    }

    public BigDecimal getNetProfit(List<IncomeStatementResponse> transactions) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (IncomeStatementResponse transaction : transactions) {
            if (transaction.getCategory() != null) {
                if (transaction.getCategory().equalsIgnoreCase("Ingresos")) {
                    totalIncome = totalIncome.add(transaction.getAmount());
                } else if (transaction.getCategory().equalsIgnoreCase("Gastos")) {
                    totalExpenses = totalExpenses.add(transaction.getAmount());
                }
            }
        }

        return totalIncome.subtract(totalExpenses);
    }
}