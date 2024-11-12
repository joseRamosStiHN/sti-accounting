package com.sti.accounting.repositories;


import com.sti.accounting.entities.BulkAccountConfig;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IBulkAccountConfigRepository extends ListCrudRepository<BulkAccountConfig, Long> {

}
