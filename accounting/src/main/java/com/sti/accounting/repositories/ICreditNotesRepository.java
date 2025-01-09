package com.sti.accounting.repositories;

import com.sti.accounting.entities.CreditNotesEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ICreditNotesRepository extends ListCrudRepository<CreditNotesEntity, Long> {

    List<CreditNotesEntity> getCreditNotesByTransactionIdAndTenantId(Long transactionId, String tenantId);

}

