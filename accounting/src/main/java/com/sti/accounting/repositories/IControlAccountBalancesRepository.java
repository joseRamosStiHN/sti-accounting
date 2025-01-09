package com.sti.accounting.repositories;

import com.sti.accounting.entities.ControlAccountBalancesEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IControlAccountBalancesRepository extends ListCrudRepository<ControlAccountBalancesEntity, Long> {

    Optional<ControlAccountBalancesEntity> findByAccountIdAndAccountingPeriodIdAndTenantId(Long accountId, Long accountingPeriodId, String tenantId);

    List<ControlAccountBalancesEntity> findAllByAccountIdAndTenantId(Long accountId, String tenantId);

    List<ControlAccountBalancesEntity> findAllByAccountingPeriodIdAndTenantId(Long accountingPeriodId, String tenantId);

    Optional<ControlAccountBalancesEntity> findByAccountIdAndCreateAtDateBetweenAndTenantId(Long accountId, LocalDate startDate, LocalDate endDate, String tenantId);

    List<ControlAccountBalancesEntity> findAllByAccountIdAndAccountingPeriodIdAndCreateAtDateBetweenAndTenantId(Long accountId, Long accountingPeriodId, LocalDate startDate, LocalDate endDate, String tenantId);

    List<ControlAccountBalancesEntity> findAllByAccountIdAndCreateAtDateBetweenAndTenantId(Long accountId, LocalDate startDate, LocalDate endDate, String tenantId);
}
