package com.sti.accounting.repositories;

import com.sti.accounting.entities.TaxSettingsEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ITaxSettingsRepository extends ListCrudRepository<TaxSettingsEntity, Long> {
}
