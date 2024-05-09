package com.sti.accounting.repositories;

import com.sti.accounting.entities.TransactionBalanceGeneralEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ITransactionBalanceGeneralRepository extends CrudRepository<TransactionBalanceGeneralEntity, Long> {

    List<TransactionBalanceGeneralEntity> findByparentId(Integer parentId);
}
