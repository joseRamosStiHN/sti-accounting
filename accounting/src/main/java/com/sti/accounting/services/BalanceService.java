package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.*;
import com.sti.accounting.utils.AppUtil;
import com.sti.accounting.utils.Motion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class BalanceService {

    private static final Logger logger = LoggerFactory.getLogger(BalanceService.class);

    private final ITransactionBalanceGeneralRepository view;

    public BalanceService( ITransactionBalanceGeneralRepository view) {
        this.view = view;
    }



    @Transactional
    public List<BalanceGeneralResponse> getBalanceGeneral() {
       try {
           List<TransactionBalanceGeneralEntity> response = new ArrayList<>();
           Iterable<TransactionBalanceGeneralEntity> data = view.findAll();
           data.forEach(response::add);
           Map<Integer, BalanceGeneralResponse> resultProcess = AppUtil.processTree(response);
           List<BalanceGeneralResponse> result = new ArrayList<>();

           Iterator<Map.Entry<Integer, BalanceGeneralResponse>> convert = resultProcess.entrySet().iterator();

           while (convert.hasNext()) {
               Map.Entry<Integer, BalanceGeneralResponse> object = convert.next();
               result.add(object.getValue());
           }
           Map<Integer, BalanceGeneralResponse> treeRoot = AppUtil.buildBalanceGeneralTree(AppUtil.buildBalanceGeneral(response), false);
           Iterator<Map.Entry<Integer, BalanceGeneralResponse>> sonSort = processResultRoot(treeRoot, resultProcess).entrySet().iterator();
           while (sonSort.hasNext()) {
               Map.Entry<Integer, BalanceGeneralResponse> object = sonSort.next();
               result.add(object.getValue());
           }
           return result;
       }catch (Exception e) {
               throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Data");
       }
    }


    private Map<Integer, BalanceGeneralResponse> processResultRoot(Map<Integer, BalanceGeneralResponse> tree, Map<Integer, BalanceGeneralResponse> sonRow) {
        Iterator<Map.Entry<Integer, BalanceGeneralResponse>> sortRoot = tree.entrySet().iterator();
        while (sortRoot.hasNext()) {
            Map.Entry<Integer, BalanceGeneralResponse> object = sortRoot.next();
            List<TransactionBalanceGeneralEntity> son = view.findByparentId(object.getKey());
            BigDecimal sumData = BigDecimal.ZERO;
            if (!son.isEmpty()) {
                for (TransactionBalanceGeneralEntity accountEntity : son) {
                    sumData = sumData.add(sonRow.get(accountEntity.getId()).getAmount());
                }
                BalanceGeneralResponse overWriteObject = object.getValue();
                overWriteObject.setAmount(overWriteObject.getAmount().add(sumData));
                tree.put(object.getKey(), overWriteObject);
            }
        }
        return tree;
    }

}
