package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingPeriodEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IAccountingPeriodRepository extends ListCrudRepository<AccountingPeriodEntity, Long> {

    Optional<AccountingPeriodEntity> findByStatusTrue();

    List<AccountingPeriodEntity> findAllByStatus(boolean status);

}
