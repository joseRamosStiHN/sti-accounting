package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountCategoryEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAccountCategoryRepository extends ListCrudRepository<AccountCategoryEntity, Long> {
}
