package com.sti.accounting.repositories;

import com.sti.accounting.entities.TransactionViewEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ITransactionViewRepository extends CrudRepository<TransactionViewEntity, Long> {

    @Query(value = "select * from db_contabilidad.book_account r where r.account_id = :account and `date`  BETWEEN  :initDate and :endDate", nativeQuery = true)
    public List<TransactionViewEntity> findTrx(@Param("account") String account, @Param("initDate") LocalDate initDate, @Param("endDate") LocalDate endDate);

}
