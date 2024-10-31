package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.utils.Status;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        List<IncomeStatementResponse> incomeStatementResponse = incomeStatementService.getIncomeStatement();
        logger.info("Generating balance general");

        // Realiza el filtro por las cuentas activas
        List<AccountEntity> accounts = iAccountRepository.findAll().stream()
                .filter(account -> account.getAccountCategory().getName().equalsIgnoreCase("Balance General") && account.getStatus() == Status.ACTIVO)
                .toList();

        List<GeneralBalanceResponse> response = accounts.stream()
                .map(this::createGeneralBalanceResponse)
                .collect(Collectors.toList());

        // Verificar si la cuenta existe y actualizar su balance
        Optional<GeneralBalanceResponse> optionalAccount = response.stream()
                .filter(item -> item.getAccountName().equals("UTILIDAD O PERDIDA NETA DEL EJERCICIO"))
                .findFirst();

        if (optionalAccount.isPresent()) {
            optionalAccount.get().setBalance(incomeStatementService.getNetProfit(incomeStatementResponse));
        } else {
            GeneralBalanceResponse netProfitResponse = new GeneralBalanceResponse();
            netProfitResponse.setAccountId(0L);
            netProfitResponse.setAccountName("UTILIDAD O PERDIDA NETA DEL EJERCICIO");
            netProfitResponse.setParentId(null);
            netProfitResponse.setCategory("PATRIMONIO");
            netProfitResponse.setBalance(incomeStatementService.getNetProfit(incomeStatementResponse));
            netProfitResponse.setRoot(true);
            response.add(netProfitResponse);
        }

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

        BigDecimal initialBalance = getInitialBalance(account.getBalances());

        item.setBalance(initialBalance.subtract(balance));
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

    private BigDecimal getInitialBalance(List<BalancesEntity> balances) {
        // Verifica si la lista de balances está vacía
        if (balances == null || balances.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Busca el balance que tiene isCurrent como true para obtener el saldo de esa cuenta
        return balances.stream()
                .filter(balance -> balance.getIsCurrent() != null && balance.getIsCurrent())
                .map(BalancesEntity::getInitialBalance)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}