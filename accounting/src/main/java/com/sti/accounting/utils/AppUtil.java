package com.sti.accounting.utils;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.entities.TransactionBalanceGeneralEntity;
import com.sti.accounting.models.BalanceGeneralResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AppUtil {

    private AppUtil() {

    }

    public static List<BalancesEntity> buildBalance(AccountEntity account) {
        List<BalancesEntity> balancesList = new ArrayList<>();
        BalancesEntity balanceDummy = new BalancesEntity();
        balanceDummy.setInitialBalance(BigDecimal.ZERO);
        balanceDummy.setIsActual(false);
        balanceDummy.setAccount(account);
        balancesList.add(balanceDummy);
        return balancesList;
    }


    public static List<BalanceGeneralResponse> buildBalanceGeneral(List<TransactionBalanceGeneralEntity> entities) {
        List<BalanceGeneralResponse> response = new ArrayList<>();
        BalanceGeneralResponse balance;
        for (TransactionBalanceGeneralEntity entity : entities) {
            balance = new BalanceGeneralResponse();
            balance.setId(entity.getId());
            balance.setAccountName(entity.getAccountName());
            balance.setParentId(entity.getParentId());
            balance.setCategory(entity.getCategory());
            balance.setRoot(entity.isRoot());
            balance.setAmount(entity.getCredit().subtract(entity.getDebit()));
            response.add(balance);
        }
        return response;
    }

}
