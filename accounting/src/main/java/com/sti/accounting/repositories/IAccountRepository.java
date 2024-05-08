package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountEntity;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IAccountRepository extends ListCrudRepository<AccountEntity, Long> {

    boolean existsByCode(String code);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM AccountEntity a " +
            "WHERE a.code = :code AND a.id <> :id")
    boolean existsByCodeAndNotId(@Param("code") String code, @Param("id") Long id);
}
