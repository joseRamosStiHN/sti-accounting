package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountEntity;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAccountRepository extends ListCrudRepository<AccountEntity, Long> {
}
