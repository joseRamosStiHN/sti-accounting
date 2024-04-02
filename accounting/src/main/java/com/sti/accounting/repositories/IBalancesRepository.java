package com.sti.accounting.repositories;

import com.sti.accounting.entities.BalancesEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IBalancesRepository extends ListCrudRepository<BalancesEntity, Long> {
}
