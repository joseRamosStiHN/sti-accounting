package com.sti.accounting.repositories;

import com.sti.accounting.entities.BalancesEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBalancesRepository extends ListCrudRepository<BalancesEntity, Long> {

    List<BalancesEntity> findByAccountId(Long accountId);

    @Query(value = "SELECT * FROM balances WHERE account_id = :accountId ORDER BY DATE DESC LIMIT 1", nativeQuery = true)
    BalancesEntity findMostRecentBalanceByAccountId(@Param("accountId") Long accountId);


}
