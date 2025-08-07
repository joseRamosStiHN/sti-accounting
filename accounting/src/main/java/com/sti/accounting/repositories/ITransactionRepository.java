package com.sti.accounting.repositories;

import com.sti.accounting.entities.TransactionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ITransactionRepository extends ListCrudRepository<TransactionEntity, Long> {

    List<TransactionEntity> findByDocumentIdAndTenantId(Long id, String tenantId);

    List<TransactionEntity> findByCreateAtDateBetweenAndTenantId(LocalDate startDate, LocalDate endDate, String tenantId);

    @Query("SELECT COUNT(td) > 0 FROM TransactionDetailEntity td WHERE td.account.id = :accountId AND td.transaction.tenantId = :tenantId")
    boolean existsByAccountIdAndTenantId(@Param("accountId") Long accountId, @Param("tenantId") String tenantId);

    @Query(value = """
            
            select a.description, a.code,\s
                       (select description from accounts where id = a.parent_id) as cuentaPadre,\s
                       t.date,\s
                       'Transacción' as movimiento,\s
                       td.motion,\s
                       td.amount,\s
                       t.number_pda,\s
                       ac.name, 
                       t.accounting_period_id
                from accounts a
                inner join transaction_detail td on a.id = td.account_id
                inner join transactions t on t.id = td.transaction_id
            	inner join account_category ac on ac.id = a.category_id
                where t.status = 'SUCCESS'
            
                UNION ALL
            
                select a.description, a.code,\s
                       (select description from accounts where id = a.parent_id) as cuentaPadre,\s
                       DATE_FORMAT(aa.creation_date,'%Y-%m-%d') as date,\s
                       'Ajuste' as movimiento,\s
                       ad.motion,\s
                       ad.amount,\s
                       t.number_pda,\s
                       ac.name,
                       aa.accounting_period_id
                from accounts a
                inner join adjustment_detail ad on a.id = ad.account_id
                inner join accounting_adjustments aa on aa.id = ad.adjustment_id
                inner join transactions t on t.id = aa.transaction_id
                inner join account_category ac on ac.id = a.category_id
                where aa.status = 'SUCCESS'
               \s
                UNION ALL
            
                select a.description, a.code,\s
                       (select description from accounts where id = a.parent_id) as cuentaPadre,\s
                       cn.date,\s
                       'Nota de Crédito' as movimiento,\s
                       cnd.motion,\s
                       cnd.amount,\s
                       t.number_pda,\s
                       ac.name,
                       cn.accounting_period_id
                from accounts a
                inner join credit_notes_detail cnd on a.id = cnd.account_id
                inner join credit_notes cn on cn.id = cnd.credit_note_id
                inner join transactions t on t.id = cn.transaction_id
               inner join account_category ac on ac.id = a.category_id
               where cn.status = 'SUCCESS'
            \s
                UNION ALL
            
                select a.description, a.code,\s
                       (select description from accounts where id = a.parent_id) as cuentaPadre,\s
                       dn.date,\s
                       'Nota de Débito' as movimiento,\s
                       dnd.motion,\s
                       dnd.amount,\s
                       t.number_pda,\s
                       ac.name,
                       dn.accounting_period_id
                from accounts a
                inner join debit_notes_detail dnd on a.id = dnd.account_id
                inner join debit_notes dn on dn.id = dnd.debit_note_id
                inner join transactions t on t.id = dn.transaction_id
                inner join account_category ac on ac.id = a.category_id
                 where dn.status = 'SUCCESS'
            """, nativeQuery = true)
    List<Object[]> getAccountTransactionSummary();

    boolean existsByReferenceAndTenantId(String reference, String tenantId);

    @Query("SELECT COUNT(t) > 0 FROM TransactionEntity t WHERE t.reference = :reference AND t.tenantId = :tenantId AND t.id != :id")
    boolean existsByReferenceAndTenantIdAndIdNot(@Param("reference") String reference,
                                                 @Param("tenantId") String tenantId,
                                                 @Param("id") Long id);
}
