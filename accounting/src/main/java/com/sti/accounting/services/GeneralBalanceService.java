package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeneralBalanceService {

    private static final Logger logger = LoggerFactory.getLogger(GeneralBalanceService.class);

    private final IAccountRepository iAccountRepository;
    private final ControlAccountBalancesService controlAccountBalancesService;
    private final IncomeStatementService incomeStatementService;

    public GeneralBalanceService(IAccountRepository iAccountRepository, ControlAccountBalancesService controlAccountBalancesService, IncomeStatementService incomeStatementService) {
        this.iAccountRepository = iAccountRepository;
        this.controlAccountBalancesService = controlAccountBalancesService;
        this.incomeStatementService = incomeStatementService;
    }

    @Transactional
    public List<GeneralBalanceResponse> getBalanceGeneral() {
        IncomeStatementResponse incomeStatementResponse = incomeStatementService.getIncomeStatement();
        logger.info("Generating balance general");

        List<AccountEntity> accounts = iAccountRepository.findAll();

        accounts = accounts.stream()
                .filter(account -> account.getAccountCategory().getName().equalsIgnoreCase("Balance General"))
                .toList();

        List<GeneralBalanceResponse> response = new ArrayList<>();

        for (AccountEntity account : accounts) {
            GeneralBalanceResponse item = createGeneralBalanceResponse(account);
            response.add(item);
        }

        // Agregar la utilidad o pérdida del ejercicio
        GeneralBalanceResponse netProfitResponse = new GeneralBalanceResponse();
        netProfitResponse.setAccountId(0L);
        netProfitResponse.setAccountName("Utilidad o Perdida del ejercicio");
        netProfitResponse.setParentId(null);
        netProfitResponse.setCategory("PATRIMONIO");
        netProfitResponse.setBalance(incomeStatementService.getNetProfit(incomeStatementResponse));
        netProfitResponse.setRoot(true);

        response.add(netProfitResponse);
        return response;
    }

    private GeneralBalanceResponse createGeneralBalanceResponse(AccountEntity account) {
        GeneralBalanceResponse item = new GeneralBalanceResponse();
        item.setAccountId(account.getId());
        item.setAccountName(account.getDescription());
        item.setParentId(account.getParent() != null ? account.getParent().getId() : null);

        String category = getCategory(account);

        item.setCategory(category);

        ControlAccountBalancesEntity sumViewEntity = controlAccountBalancesService.getControlAccountBalances(account.getId());

        BigDecimal balance = getBalance(sumViewEntity);

        item.setBalance(balance);
        item.setRoot(account.getParent() == null);

        return item;
    }

    private String getCategory(AccountEntity account) {
        String[] mainCategories = {"ACTIVO", "PASIVO", "PATRIMONIO"};
        AccountEntity current = account;
        while (current != null) {
            for (String mainCategory : mainCategories) {
                if (current.getDescription().toUpperCase().contains(mainCategory)) {
                    return mainCategory;
                }
            }
            current = current.getParent();
        }
        return "";
    }


    private BigDecimal getBalance(ControlAccountBalancesEntity sumViewEntity) {
        BigDecimal debit = sumViewEntity.getDebit() != null ? new BigDecimal(sumViewEntity.getDebit()) : BigDecimal.ZERO;
        BigDecimal credit = sumViewEntity.getCredit() != null ? new BigDecimal(sumViewEntity.getCredit()) : BigDecimal.ZERO;
        return debit.subtract(credit).abs();
    }
}