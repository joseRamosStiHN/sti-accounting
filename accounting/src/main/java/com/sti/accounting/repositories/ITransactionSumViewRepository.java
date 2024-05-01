package com.sti.accounting.repositories;

import com.sti.accounting.entities.TransactionSumViewEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ITransactionSumViewRepository extends CrudRepository<TransactionSumViewEntity, Long> {

    @Query(value = "SELECT r.id , sum(r.debit) as debit, sum(r.credit) as credit  from db_contabilidad.book_account_sum r where r.account_id = :account and `date`  BETWEEN  :initDate and :endDate", nativeQuery = true)
    List<TransactionSumViewEntity> FindTrx(@Param("account") String account, @Param("initDate") LocalDate initDate, @Param("endDate") LocalDate endDate);

}
