package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingClosingEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAccountingClosingRepository extends ListCrudRepository<AccountingClosingEntity, Long> {
}
