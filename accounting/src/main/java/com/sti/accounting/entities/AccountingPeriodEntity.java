package com.sti.accounting.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_period")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccountingPeriodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PERIOD_NAME")
    private String periodName;

    @Column(name = "CLOSURE_TYPE", nullable = false)
    private String closureType;

    @Column(name = "START_PERIOD", nullable = false)
    private LocalDateTime startPeriod;

    @Column(name = "END_PERIOD")
    private LocalDateTime endPeriod;

    @Column(name = "DAYS_PERIOD")
    private Integer daysPeriod;

    @Column(name = "STATUS")
    private boolean status;
}
