package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountTypeEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAccountTypeRepository extends ListCrudRepository<AccountTypeEntity, Long> {
}
