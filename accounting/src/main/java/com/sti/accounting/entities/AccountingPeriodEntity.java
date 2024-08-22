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

    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @Column(name = "CLOSURE_TYPE", nullable = false)
    private String closureType;

    @Column(name = "START_DATE", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "STATUS")
    private String status;
}
