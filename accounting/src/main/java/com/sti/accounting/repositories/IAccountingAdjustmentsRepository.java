package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingAdjustmentsEntity;
import com.sti.accounting.models.StatusTransaction;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface IAccountingAdjustmentsRepository extends ListCrudRepository<AccountingAdjustmentsEntity, Long> {

    List<AccountingAdjustmentsEntity> getAccountingAdjustmentsByTransactionIdAndTenantId(Long transactionId, String tenantId);

    List<AccountingAdjustmentsEntity> findByTenantIdAndAccountingPeriodId(String tenantId, Long periodId);

    List<AccountingAdjustmentsEntity> findByIdInAndStatus(List<Long> ids, StatusTransaction status);

    AccountingAdjustmentsEntity findByTenantIdAndId(String tenantId, Long id);

}
