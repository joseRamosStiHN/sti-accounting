package com.sti.accounting.repositories;

import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.utils.PeriodStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IAccountingPeriodRepository extends ListCrudRepository<AccountingPeriodEntity, Long> {

    @Query(value = "SELECT * FROM accounting_period WHERE period_status = 'ACTIVE' AND tenant_id = :tenantId", nativeQuery = true)
    List<AccountingPeriodEntity> findActivePeriods(@Param("tenantId") String tenantId);

    @Query("SELECT a FROM AccountingPeriodEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (a.startPeriod <= :endDate AND a.endPeriod >= :startDate)")
    List<AccountingPeriodEntity> findOverlappingPeriods(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("tenantId") String tenantId
    );
    
    @Query(value = "SELECT * FROM accounting_period WHERE period_status = 'CLOSED' AND tenant_id = :tenantId", nativeQuery = true)
    List<AccountingPeriodEntity> findByPeriodStatus(@Param("tenantId") String tenantId);

    @Query("SELECT p FROM AccountingPeriodEntity p WHERE p.startPeriod > :endPeriod AND p.closureType = :closureType AND p.periodStatus = 'INACTIVE' AND p.tenantId = :tenantId ORDER BY p.startPeriod ASC")
    List<AccountingPeriodEntity> findNextPeriod(
            @Param("endPeriod") LocalDateTime endPeriod,
            @Param("closureType") String closureType,
            @Param("tenantId") String tenantId
    );

    @Query("SELECT ap FROM AccountingPeriodEntity ap " +
            "WHERE ap.closureType = :closureType " +
            "AND ap.periodOrder = :periodOrder " +
            "AND YEAR(ap.startPeriod) = :year " +
            "AND ap.tenantId = :tenantId")
    AccountingPeriodEntity findByClosureTypeAndPeriodOrderForYear(
            @Param("closureType") String closureType,
            @Param("periodOrder") int periodOrder,
            @Param("year") int year,
            @Param("tenantId") String tenantId
    );

    boolean existsByClosureTypeAndStartPeriodAndTenantId(
            String closureType,
            LocalDateTime startPeriod,
            String tenantId
    );

    boolean existsByClosureTypeAndStartPeriodAndIdNotAndTenantId(
            String closureType,
            LocalDateTime startPeriod,
            Long id,
            String tenantId
    );

    List<AccountingPeriodEntity> findByTenantIdAndStartPeriodBetween(String tenantId, LocalDateTime startDate, LocalDateTime endDate);

    List<AccountingPeriodEntity> findByClosureTypeAndPeriodStatusAndTenantId(
            String closureType,
            PeriodStatus periodStatus,
            String tenantId
    );
}