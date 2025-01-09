package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.repositories.IControlAccountBalancesRepository;
import com.sti.accounting.utils.Motion;
import com.sti.accounting.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ControlAccountBalancesService {

    private static final Logger logger = LoggerFactory.getLogger(ControlAccountBalancesService.class);

    private final IControlAccountBalancesRepository controlAccountBalancesRepository;
    private final AccountingPeriodService accountingPeriodService;

    public ControlAccountBalancesService(IControlAccountBalancesRepository controlAccountBalancesRepository, AccountingPeriodService accountingPeriodService) {
        this.controlAccountBalancesRepository = controlAccountBalancesRepository;
        this.accountingPeriodService = accountingPeriodService;
    }

    private String getTenantId() {
        return TenantContext.getCurrentTenant();
    }

    @Transactional
    public void updateControlAccountBalances(TransactionEntity transactionEntity) {
        String tenantId = getTenantId();

        List<TransactionDetailEntity> transactionDetails = transactionEntity.getTransactionDetail();
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        for (TransactionDetailEntity detail : transactionDetails) {
            Long accountId = detail.getAccount().getId();
            BigDecimal amount = detail.getAmount();
            Motion motion = detail.getMotion();

            // Obtener el mes y año de la fecha de la transacción
            LocalDate transactionDate = detail.getTransaction().getCreateAtDate();
            LocalDate startOfMonth = transactionDate.withDayOfMonth(1);
            LocalDate endOfMonth = transactionDate.withDayOfMonth(transactionDate.lengthOfMonth());

            // Buscar el registro existente para el mes actual
            ControlAccountBalancesEntity balanceEntity = controlAccountBalancesRepository
                    .findByAccountIdAndCreateAtDateBetweenAndTenantId(accountId, startOfMonth, endOfMonth, tenantId)
                    .orElseGet(() -> {
                        ControlAccountBalancesEntity newEntity = new ControlAccountBalancesEntity();
                        newEntity.setAccountId(accountId);
                        newEntity.setAccountingPeriod(activePeriod);
                        newEntity.setCreateAtDate(detail.getTransaction().getCreateAtDate());
                        newEntity.setTenantId(tenantId);

                        return newEntity;
                    });

            // Actualizar los débitos y créditos
            if (motion.equals(Motion.D)) {
                balanceEntity.setDebit(balanceEntity.getDebit() == null ? amount :
                        (balanceEntity.getDebit()).add(amount));
            } else {
                balanceEntity.setCredit(balanceEntity.getCredit() == null ? amount :
                        (balanceEntity.getCredit()).add(amount));
            }

            // Guardar el balance actualizado
            controlAccountBalancesRepository.save(balanceEntity);
        }
    }


    public void updateControlAccountBalancesAdjustment(AccountingAdjustmentsEntity accountingAdjustmentsEntity) {
        String tenantId = getTenantId();

        List<AdjustmentDetailEntity> adjustmentDetail = accountingAdjustmentsEntity.getAdjustmentDetail();
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        for (AdjustmentDetailEntity detail : adjustmentDetail) {
            Long accountId = detail.getAccount().getId();
            BigDecimal amount = detail.getAmount();
            Motion motion = detail.getMotion();

            ControlAccountBalancesEntity sumViewEntity = controlAccountBalancesRepository.findByAccountIdAndAccountingPeriodIdAndTenantId(accountId, activePeriod.getId(), tenantId)
                    .orElseGet(() -> {
                        ControlAccountBalancesEntity newEntity = new ControlAccountBalancesEntity();
                        newEntity.setAccountId(accountId);
                        newEntity.setAccountingPeriod(activePeriod);
                        newEntity.setCreateAtDate(detail.getAdjustment().getTransaction().getCreateAtDate());
                        newEntity.setTenantId(tenantId);

                        return newEntity;
                    });


            if (motion.equals(Motion.D)) {
                sumViewEntity.setDebit(sumViewEntity.getDebit() == null ? amount :
                        sumViewEntity.getDebit().add(amount));
            } else {
                sumViewEntity.setCredit(sumViewEntity.getCredit() == null ? amount :
                        sumViewEntity.getCredit().add(amount));
            }

            controlAccountBalancesRepository.save(sumViewEntity);
        }
    }

    @Transactional
    public void updateControlAccountDebitNotes(DebitNotesEntity debitNotesEntity) {
        logger.info("creating update Control Account Balances");
        String tenantId = getTenantId();

        List<DebitNotesDetailEntity> debitNotesDetails = debitNotesEntity.getDebitNoteDetail();
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        for (DebitNotesDetailEntity detail : debitNotesDetails) {
            Long accountId = detail.getAccount().getId();
            BigDecimal amount = detail.getAmount();
            Motion motion = detail.getMotion();

            ControlAccountBalancesEntity sumViewEntity = controlAccountBalancesRepository.findByAccountIdAndAccountingPeriodIdAndTenantId(accountId, activePeriod.getId(), tenantId)
                    .orElseGet(() -> {
                        ControlAccountBalancesEntity newEntity = new ControlAccountBalancesEntity();
                        newEntity.setAccountId(accountId);
                        newEntity.setAccountingPeriod(activePeriod);
                        newEntity.setCreateAtDate(detail.getDebitNote().getCreateAtDate());
                        newEntity.setTenantId(tenantId);

                        return newEntity;
                    });


            if (motion.equals(Motion.D)) {
                sumViewEntity.setDebit(sumViewEntity.getDebit() == null ? amount :
                        sumViewEntity.getDebit().add(amount));
            } else {
                sumViewEntity.setCredit(sumViewEntity.getCredit() == null ? amount :
                        sumViewEntity.getCredit().add(amount));
            }

            controlAccountBalancesRepository.save(sumViewEntity);
        }
    }

    @Transactional
    public void updateControlAccountCreditNotes(CreditNotesEntity creditNotesEntity) {
        logger.info("creating update Control Account Balances");
        String tenantId = getTenantId();

        List<CreditNotesDetailEntity> creditNotesDetails = creditNotesEntity.getCreditNoteDetail();
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        for (CreditNotesDetailEntity detail : creditNotesDetails) {
            Long accountId = detail.getAccount().getId();
            BigDecimal amount = detail.getAmount();
            Motion motion = detail.getMotion();

            ControlAccountBalancesEntity sumViewEntity = controlAccountBalancesRepository.findByAccountIdAndAccountingPeriodIdAndTenantId(accountId, activePeriod.getId(), tenantId)
                    .orElseGet(() -> {
                        ControlAccountBalancesEntity newEntity = new ControlAccountBalancesEntity();
                        newEntity.setAccountId(accountId);
                        newEntity.setAccountingPeriod(activePeriod);
                        newEntity.setCreateAtDate(detail.getCreditNote().getCreateAtDate());
                        newEntity.setTenantId(tenantId);
                        return newEntity;
                    });


            if (motion.equals(Motion.D)) {
                sumViewEntity.setDebit(sumViewEntity.getDebit() == null ? amount :
                        sumViewEntity.getDebit().add(amount));
            } else {
                sumViewEntity.setCredit(sumViewEntity.getCredit() == null ? amount :
                        sumViewEntity.getCredit().add(amount));
            }

            controlAccountBalancesRepository.save(sumViewEntity);
        }
    }


    public List<ControlAccountBalancesEntity> getControlAccountBalancesForAllPeriods(Long accountId) {
        String tenantId = getTenantId();
        return controlAccountBalancesRepository.findAllByAccountIdAndTenantId(accountId, tenantId);
    }

    public List<ControlAccountBalancesEntity> getControlAccountBalancesForPeriodAndMonth(Long accountId, Long accountingPeriodId, LocalDate startDate, LocalDate endDate) {
        String tenantId = getTenantId();
        return controlAccountBalancesRepository.findAllByAccountIdAndAccountingPeriodIdAndCreateAtDateBetweenAndTenantId(accountId, accountingPeriodId, startDate, endDate, tenantId);
    }

    public List<ControlAccountBalancesEntity> getControlAccountBalancesForDateRange(Long accountId, LocalDate startDate, LocalDate endDate) {
        String tenantId = getTenantId();
        return controlAccountBalancesRepository.findAllByAccountIdAndCreateAtDateBetweenAndTenantId(accountId, startDate, endDate, tenantId);
    }
}
