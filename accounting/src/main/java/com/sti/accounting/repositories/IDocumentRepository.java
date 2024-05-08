package com.sti.accounting.repositories;

import com.sti.accounting.entities.DocumentEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IDocumentRepository extends ListCrudRepository<DocumentEntity, Long> {
}
