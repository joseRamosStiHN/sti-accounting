package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingPeriodEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IAccountingPeriodRepository extends ListCrudRepository<AccountingPeriodEntity, Long> {

    @Query(value = "SELECT * FROM accounting_period WHERE period_status = 'ACTIVE'", nativeQuery = true)
    List<AccountingPeriodEntity> findActivePeriods();

    List<AccountingPeriodEntity> findByStartPeriodBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = "SELECT * FROM accounting_period WHERE period_status = 'CLOSED'", nativeQuery = true)
    List<AccountingPeriodEntity> findByPeriodStatus();

    @Query("SELECT p FROM AccountingPeriodEntity p WHERE p.startPeriod > :endPeriod AND p.closureType = :closureType AND p.periodStatus = 'INACTIVE' ORDER BY p.startPeriod ASC")
    List<AccountingPeriodEntity> findNextPeriod(@Param("endPeriod") LocalDateTime endPeriod, @Param("closureType") String closureType);

    AccountingPeriodEntity findByPeriodOrder(int periodOrder);

}
