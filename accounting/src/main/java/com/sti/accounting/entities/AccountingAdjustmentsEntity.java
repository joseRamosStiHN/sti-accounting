package com.sti.accounting.entities;

import com.sti.accounting.models.StatusTransaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "accounting_adjustments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountingAdjustmentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "TRANSACTION_ID", nullable = false)
    private TransactionEntity transaction;

    @Column(name = "REFERENCE")
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private StatusTransaction status;

    @CreationTimestamp
    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;

    @OneToMany(mappedBy = "adjustment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdjustmentDetailEntity> adjustmentDetail;


}
