package com.sti.accounting.repositories;

import com.sti.accounting.entities.ControlAccountBalancesEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IControlAccountBalancesRepository extends ListCrudRepository<ControlAccountBalancesEntity, Long> {

    Optional<ControlAccountBalancesEntity> findByAccountId(Long accountId);

    Optional<ControlAccountBalancesEntity> findByAccountIdAndAccountingPeriodId(Long accountId, Long accountingPeriodId);

    List<ControlAccountBalancesEntity> findAllByAccountId(Long accountId);
}
