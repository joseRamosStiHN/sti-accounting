package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingJournalEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface IAccountingJournalRepository extends ListCrudRepository<AccountingJournalEntity, Long> {

    boolean existsByAccountType_IdAndTenantIdAndStatus(BigDecimal accountTypeId, String tenantId,boolean status);

}
