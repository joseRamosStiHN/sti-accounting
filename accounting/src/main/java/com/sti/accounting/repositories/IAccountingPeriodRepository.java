package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingPeriodEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IAccountingPeriodRepository extends ListCrudRepository<AccountingPeriodEntity, Long> {

    @Query(value = "SELECT * FROM accounting_period WHERE period_status = 'ACTIVE'", nativeQuery = true)
    List<AccountingPeriodEntity> findActivePeriods();

    List<AccountingPeriodEntity> findByStartPeriodBetween(LocalDateTime startDate, LocalDateTime endDate);

    AccountingPeriodEntity findByPeriodOrder(int periodOrder);

}
