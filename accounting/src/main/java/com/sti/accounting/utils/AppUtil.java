package com.sti.accounting.utils;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.entities.TransactionBalanceGeneralEntity;
import com.sti.accounting.models.BalanceGeneralResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

public class AppUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppUtil.class);


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

    public static Map<Integer, BalanceGeneralResponse> buildBalanceGeneralTree(List<BalanceGeneralResponse> entities, boolean root) {
        LinkedHashMap<Integer, BalanceGeneralResponse> pair = new LinkedHashMap<>();
        if (root) {
            for (BalanceGeneralResponse entity : entities) {
                if (!entity.isRoot() && entity.getParentId() != null) {
                    pair.put(entity.getId(), entity);
                }
            }
        } else {
            for (BalanceGeneralResponse entity : entities) {
                if (entity.isRoot() && entity.getParentId() == null) {
                    pair.put(entity.getId(), entity);
                }
            }
        }
        return pair;
    }

    private static Map<Integer, Integer> createTreeParents(List<TransactionBalanceGeneralEntity> list) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (TransactionBalanceGeneralEntity entity : list) {
            result.merge(entity.getParentId(), 1, Integer::sum);
        }
        return result;
    }


    public static Map<Integer, BalanceGeneralResponse> processTree(List<TransactionBalanceGeneralEntity> list) {
        Map<Integer, BalanceGeneralResponse> treeIteration = buildBalanceGeneralTree(AppUtil.buildBalanceGeneral(list), true);

        Map<Integer, Integer> parent = createTreeParents(list);

        Map<Integer, BalanceGeneralResponse> treeFinal = new LinkedHashMap<>();

        ////LOOP
        do {
            int key = findSheet(treeIteration, parent);
            if (key != 0) {
                int keyParent = treeIteration.get(key).getParentId();
                if (treeIteration.get(keyParent) != null) {
                    BigDecimal valueKey = treeIteration.get(key).getAmount();
                    BigDecimal newAmount = treeIteration.get(keyParent).getAmount().add(valueKey);
                    treeIteration.get(keyParent).setAmount(newAmount);
                    treeFinal.put(keyParent, treeIteration.get(keyParent));

                }
                if (parent.get(keyParent) != null) {
                    if (parent.get(keyParent) == 1) {
                        parent.remove(keyParent);
                    } else {
                        parent.put(keyParent, parent.get(keyParent) - 1);
                    }
                }
                treeFinal.put(key, treeIteration.get(key));
                treeIteration.remove(key);
            }

        } while (!treeIteration.isEmpty());
        LOGGER.info("Done");
        return treeFinal;
    }

    private static int findSheet(Map<Integer, BalanceGeneralResponse> treeIteration, Map<Integer, Integer> parent) {
        for (Map.Entry<Integer, BalanceGeneralResponse> object : treeIteration.entrySet()) {
            if (parent.get(object.getKey()) == null) {
                return object.getKey();
            }
        }

        return 0;

    }
}
