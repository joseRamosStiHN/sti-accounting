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

    public IncomeStatementResponse getIncomeStatement() {
        logger.info("Generating income statement");

        List<AccountEntity> accounts = accountRepository.findAll();

        accounts = accounts.stream()
                .filter(account -> account.getAccountCategory().getName().equalsIgnoreCase("Estado de Resultados"))
                .toList();

        List<IncomeStatementResponse.Transaction> transactions = new ArrayList<>();

        for (AccountEntity account : accounts) {
            ControlAccountBalancesEntity sumViewEntity = controlAccountBalancesService.getControlAccountBalances(account.getId());
            BigDecimal balance = getBalance(sumViewEntity);

            IncomeStatementResponse.Transaction transaction = new IncomeStatementResponse.Transaction();
            transaction.setId(account.getId());
            transaction.setCategory(account.getAccountType().getName());
            transaction.setAccountParent(account.getParent() != null ? account.getParent().getDescription() : null);
            transaction.setAccount(account.getDescription());
            transaction.setAmount(balance);
            transaction.setDate(new Date());

            transactions.add(transaction);
        }

        IncomeStatementResponse response = new IncomeStatementResponse();
        response.setTransactions(transactions);

        return response;
    }

    private BigDecimal getBalance(ControlAccountBalancesEntity sumViewEntity) {
        BigDecimal debit = sumViewEntity.getDebit() != null ? new BigDecimal(sumViewEntity.getDebit()) : BigDecimal.ZERO;
        BigDecimal credit = sumViewEntity.getCredit() != null ? new BigDecimal(sumViewEntity.getCredit()) : BigDecimal.ZERO;
        return debit.subtract(credit).abs();
    }
}