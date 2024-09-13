package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingAdjustmentsEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface IAccountingAdjustmentsRepository extends ListCrudRepository<AccountingAdjustmentsEntity, Long> {

}
