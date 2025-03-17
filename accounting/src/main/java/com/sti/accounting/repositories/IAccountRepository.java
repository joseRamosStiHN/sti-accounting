package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountEntity;

import com.sti.accounting.utils.Status;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IAccountRepository extends ListCrudRepository<AccountEntity, Long> {

    boolean existsByCodeAndTenantId(String code, String tenantId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM AccountEntity a " +
            "WHERE a.code = :code AND a.id <> :id AND a.tenantId = :tenantId")
    boolean existsByCodeAndNotId(@Param("code") String code, @Param("id") Long id, @Param("tenantId") String tenantId);

    @Query("SELECT a FROM AccountEntity a WHERE a.accountCategory.name = :categoryName AND a.status = :status AND a.tenantId = :tenantId")
    List<AccountEntity> findFilteredAccounts(
            @Param("categoryName") String categoryName,
            @Param("status") Status status,
            @Param("tenantId") String tenantId);

    long countByParentIdAndTenantId(Long parentId, String tenantId);

    List<AccountEntity> findAllByTenantId(String tenantId);

}
