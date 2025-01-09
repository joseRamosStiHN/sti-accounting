package com.sti.accounting.entities;

import com.sti.accounting.models.StatusTransaction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "accounting_adjustments")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingAdjustmentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "TRANSACTION_ID", nullable = false)
    private TransactionEntity transaction;

    @Column(name = "DESCRIPTION_ADJUSTMENT")
    private String descriptionAdjustment;

    @Column(name = "REFERENCE")
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private StatusTransaction status;

    @CreationTimestamp
    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;

    @ManyToOne
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriodEntity accountingPeriod;

    @OneToMany(mappedBy = "adjustment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdjustmentDetailEntity> adjustmentDetail;

    @Column(name = "TENANT_ID")
    private String tenantId;
}
