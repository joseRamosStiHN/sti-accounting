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

    public GeneralBalanceService(IAccountRepository iAccountRepository, ControlAccountBalancesService controlAccountBalancesService) {
        this.iAccountRepository = iAccountRepository;
        this.controlAccountBalancesService = controlAccountBalancesService;
    }

    @Transactional
    public List<GeneralBalanceResponse> getBalanceGeneral() {
        logger.info("Generating balance general");

        List<AccountEntity> accounts = iAccountRepository.findAll();

        List<GeneralBalanceResponse> response = new ArrayList<>();

        for (AccountEntity account : accounts) {
            GeneralBalanceResponse item = createGeneralBalanceResponse(account);
            response.add(item);
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

        item.setBalance(balance);
        item.setRoot(account.getParent() == null);

        return item;
    }

    private String getCategory(AccountEntity account) {
        String parentName = account.getParent() != null ? account.getParent().getDescription() : null;
        if (parentName != null) {
            if (isMainCategory(parentName)) {
                return parentName;
            } else {
                return getCategory(account.getParent());
            }
        } else {
            return "ACTIVO";
        }
    }

    private boolean isMainCategory(String category) {
        return category.equalsIgnoreCase("ACTIVO") || category.equalsIgnoreCase("PASIVO") || category.equalsIgnoreCase("PATRIMONIO");
    }

    private BigDecimal getBalance(ControlAccountBalancesEntity sumViewEntity) {
        BigDecimal debit = sumViewEntity.getDebit() != null ? new BigDecimal(sumViewEntity.getDebit()) : BigDecimal.ZERO;
        BigDecimal credit = sumViewEntity.getCredit() != null ? new BigDecimal(sumViewEntity.getCredit()) : BigDecimal.ZERO;
        return debit.subtract(credit).abs();
    }
}