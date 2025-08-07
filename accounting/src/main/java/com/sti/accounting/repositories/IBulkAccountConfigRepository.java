package com.sti.accounting.repositories;


import com.sti.accounting.entities.BulkAccountConfig;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBulkAccountConfigRepository extends ListCrudRepository<BulkAccountConfig, Long> {


    BulkAccountConfig findByName(String name);

    List<BulkAccountConfig> findByTenantId(String tenantId);

    BulkAccountConfig findByIdAndTenantId(Long id, String tenantId);


}
