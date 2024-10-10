package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.repositories.IControlAccountBalancesRepository;
import com.sti.accounting.utils.Motion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ControlAccountBalancesService {

    private static final Logger logger = LoggerFactory.getLogger(ControlAccountBalancesService.class);

    private final IControlAccountBalancesRepository controlAccountBalancesRepository;

    public ControlAccountBalancesService(IControlAccountBalancesRepository controlAccountBalancesRepository) {
        this.controlAccountBalancesRepository = controlAccountBalancesRepository;
    }

    @Transactional
    public void updateControlAccountBalances(TransactionEntity transactionEntity) {
        logger.info("creating update Control Account Balances");

        List<TransactionDetailEntity> transactionDetails = transactionEntity.getTransactionDetail();

        for (TransactionDetailEntity detail : transactionDetails) {
            Long accountId = detail.getAccount().getId();
            BigDecimal amount = detail.getAmount();
            Motion motion = detail.getMotion();

            ControlAccountBalancesEntity sumViewEntity = controlAccountBalancesRepository.findByAccountId(accountId)
                    .orElseGet(() -> {
                        ControlAccountBalancesEntity newEntity = new ControlAccountBalancesEntity();
                        newEntity.setAccountId(accountId);
                        return newEntity;
                    });


            if (motion.equals(Motion.D)) {
                sumViewEntity.setDebit(sumViewEntity.getDebit() == null ? amount.toString() :
                        new BigDecimal(sumViewEntity.getDebit()).add(amount).toString());
            } else {
                sumViewEntity.setCredit(sumViewEntity.getCredit() == null ? amount.toString() :
                        new BigDecimal(sumViewEntity.getCredit()).add(amount).toString());
            }


            controlAccountBalancesRepository.save(sumViewEntity);
        }
    }

    public void updateControlAccountBalancesAdjustment(AccountingAdjustmentsEntity accountingAdjustmentsEntity) {
        List<AdjustmentDetailEntity> adjustmentDetail = accountingAdjustmentsEntity.getAdjustmentDetail();

        for (AdjustmentDetailEntity detail : adjustmentDetail) {
            Long accountId = detail.getAccount().getId();
            BigDecimal amount = detail.getAmount();
            Motion motion = detail.getMotion();

            ControlAccountBalancesEntity sumViewEntity = controlAccountBalancesRepository.findByAccountId(accountId)
                    .orElseGet(() -> {
                        ControlAccountBalancesEntity newEntity = new ControlAccountBalancesEntity();
                        newEntity.setAccountId(accountId);
                        return newEntity;
                    });


            if (motion.equals(Motion.D)) {
                sumViewEntity.setDebit(sumViewEntity.getDebit() == null ? amount.toString() :
                        new BigDecimal(sumViewEntity.getDebit()).add(amount).toString());
            } else {
                sumViewEntity.setCredit(sumViewEntity.getCredit() == null ? amount.toString() :
                        new BigDecimal(sumViewEntity.getCredit()).add(amount).toString());
            }

            controlAccountBalancesRepository.save(sumViewEntity);
        }
    }

    @Transactional
    public void updateControlAccountDebitNotes(DebitNotesEntity debitNotesEntity) {
        logger.info("creating update Control Account Balances");

        List<DebitNotesDetailEntity> debitNotesDetails = debitNotesEntity.getDebitNoteDetail();

        for (DebitNotesDetailEntity detail : debitNotesDetails) {
            Long accountId = detail.getAccount().getId();
            BigDecimal amount = detail.getAmount();
            Motion motion = detail.getMotion();

            ControlAccountBalancesEntity sumViewEntity = controlAccountBalancesRepository.findByAccountId(accountId)
                    .orElseGet(() -> {
                        ControlAccountBalancesEntity newEntity = new ControlAccountBalancesEntity();
                        newEntity.setAccountId(accountId);
                        return newEntity;
                    });


            if (motion.equals(Motion.D)) {
                sumViewEntity.setDebit(sumViewEntity.getDebit() == null ? amount.toString() :
                        new BigDecimal(sumViewEntity.getDebit()).add(amount).toString());
            } else {
                sumViewEntity.setCredit(sumViewEntity.getCredit() == null ? amount.toString() :
                        new BigDecimal(sumViewEntity.getCredit()).add(amount).toString());
            }


            controlAccountBalancesRepository.save(sumViewEntity);
        }
    }

    public ControlAccountBalancesEntity getControlAccountBalances(Long accountId) {
        return controlAccountBalancesRepository.findByAccountId(accountId)
                .orElseGet(() -> {
                    ControlAccountBalancesEntity newEntity = new ControlAccountBalancesEntity();
                    newEntity.setAccountId(accountId);
                    return newEntity;
                });
    }
}
