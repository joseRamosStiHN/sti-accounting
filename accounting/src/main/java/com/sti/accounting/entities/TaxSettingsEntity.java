package com.sti.accounting.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_settings")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TaxSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TAX_RATE")
    private String taxRate;

    @Column(name = "TYPE", columnDefinition = "TEXT")
    private String type;

    @Column(name = "FROM_VALUE")
    private BigDecimal fromValue;

    @Column(name = "TO_VALUE")
    private BigDecimal toValue;

    @Column(name = "IS_CURRENT")
    private Boolean isCurrent;

    @CreationTimestamp
    @Column(name = "DATE")
    private LocalDateTime createAtDate;
}
