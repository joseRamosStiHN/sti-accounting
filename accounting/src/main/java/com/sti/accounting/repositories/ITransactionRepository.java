package com.sti.accounting.repositories;

import com.sti.accounting.entities.TransactionEntity;
import com.sti.accounting.models.AccountTransactionDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface ITransactionRepository extends ListCrudRepository<TransactionEntity, Long> {

    List<TransactionEntity> findByDocumentId(Long id);

    List<TransactionEntity> findByCreateAtDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(td) > 0 FROM TransactionDetailEntity td WHERE td.account.id = :accountId")
    boolean existsByAccountId(@Param("accountId") Long accountId);

    @Query(value = "select a.description, a.code, (\n" +
            "select description from accounts where id= a.parent_id\n" +
            ") as cuentaPadre ,t.date, 'transaction' as movimiento, td.motion, td.amount, t.number_pda, ac.name\n" +
            "from accounts a\n" +
            "inner join transaction_detail td on a.id = td.account_id\n" +
            "inner join transactions t on t.id = td.transaction_id\n" +
            "inner join account_category ac on ac.id = a.account_type_id",
            nativeQuery = true)
    List<Object[]> getAccountTransactionSummary();
}
