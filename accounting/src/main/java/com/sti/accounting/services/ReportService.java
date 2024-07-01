package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.StreamSupport;


@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final ITransactionBalanceGeneralRepository view;

    public ReportService(ITransactionBalanceGeneralRepository view) {
        this.view = view;
    }

    private BalanceGeneralResponse toDto(TransactionBalanceGeneralEntity entity) {
        BalanceGeneralResponse dto = new BalanceGeneralResponse();
        dto.setId(entity.getId());
        dto.setCategory(entity.getCategory());
        dto.setAmount(entity.getCredit().subtract(entity.getDebit()));
        dto.setRoot(entity.isRoot());
        dto.setAccountName(entity.getAccountName());
        dto.setParentId(entity.getParentId());
        return dto;
    }

    public List<BalanceGeneralResponse> getBalanceGeneral() {
        long time = System.currentTimeMillis();
        logger.info("Init Balance General");
        var entities = StreamSupport.stream(view.findAll().spliterator(), false).toList();
        List<BalanceGeneralResponse> roots = entities.stream().filter(TransactionBalanceGeneralEntity::isRoot).map(this::toDto).toList();
        List<BalanceGeneralResponse> otherLevels = entities.stream().filter(x -> !x.isRoot() && x.getParentId() != null).map(this::toDto).toList();
        Map<Integer, BalanceGeneralResponse> parents = new HashMap<>();
        for (BalanceGeneralResponse other : otherLevels) {
            parents.put(other.getId(), other);
        }
        for (BalanceGeneralResponse item : otherLevels) {
            Integer parentId = item.getParentId();
            if (parents.containsKey(parentId)) {
                BalanceGeneralResponse current = parents.get(parentId);
                BigDecimal amount = current.getAmount().add(item.getAmount());
                current.setAmount(amount);
            }
        }

        List<BalanceGeneralResponse> data = new ArrayList<>(roots);
        List<BalanceGeneralResponse> values = parents.values().stream().toList();
        data.addAll(values);

        Map<Integer, BalanceGeneralResponse> rootMaps = new HashMap<>();
        for (BalanceGeneralResponse root : data) {
            rootMaps.put(root.getId(), root);
        }

        for (BalanceGeneralResponse root : data) {
            if (root.getParentId() != null) {
                BalanceGeneralResponse parent = rootMaps.get(root.getParentId());
                BigDecimal amount = parent.getAmount().add(root.getAmount());
                parent.setAmount(amount);
            }
        }
        logger.info("Done Balance General : Process Time  {}", System.currentTimeMillis() - time );
        return rootMaps.values().stream().toList();
    }


}
