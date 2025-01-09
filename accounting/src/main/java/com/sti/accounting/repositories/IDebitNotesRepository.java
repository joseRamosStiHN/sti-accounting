package com.sti.accounting.repositories;

import com.sti.accounting.entities.DebitNotesEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IDebitNotesRepository extends ListCrudRepository<DebitNotesEntity, Long> {

    List<DebitNotesEntity> getDebitNotesByTransactionIdAndTenantId(Long transactionId, String tenantId);

}
