package com.sti.accounting.repositories;

import com.sti.accounting.entities.TransactionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ITransactionRepository extends ListCrudRepository<TransactionEntity, Long> {

    List<TransactionEntity> findByDocumentId(Long id);

    List<TransactionEntity> findByCreateAtDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(td) > 0 FROM TransactionDetailEntity td WHERE td.account.id = :accountId")
    boolean existsByAccountId(@Param("accountId") Long accountId);
}
