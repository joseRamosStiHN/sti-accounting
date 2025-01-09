package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingAdjustmentsEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface IAccountingAdjustmentsRepository extends ListCrudRepository<AccountingAdjustmentsEntity, Long> {

    List<AccountingAdjustmentsEntity> getAccountingAdjustmentsByTransactionIdAndTenantId(Long transactionId, String tenantId);

}
