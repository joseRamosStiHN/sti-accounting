package com.sti.accounting.repositories;

import com.sti.accounting.entities.TransactionEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ITransactionRepository extends ListCrudRepository<TransactionEntity, Long> {
  List<TransactionEntity> findByDocumentId(Long id);
}
