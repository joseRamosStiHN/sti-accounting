package com.sti.accounting.repositories;

import com.sti.accounting.entities.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ITransactionRepository extends JpaRepository<TransactionEntity, Long> {
}
