package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingPeriodEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAccountingPeriodRepository extends ListCrudRepository<AccountingPeriodEntity, Long> {
}
