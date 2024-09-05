package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingJournalEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAccountingJournalRepository extends ListCrudRepository<AccountingJournalEntity, Long> {
}
