package com.sti.accounting.repositories;

import com.sti.accounting.entities.CompanyEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICompanyRepository extends ListCrudRepository<CompanyEntity, Long> {

    CompanyEntity findByTenantId(String tenantId);
}


