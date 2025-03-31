package com.sti.accounting.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pda_sequence")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PdaSequenceEntity {

    @Id
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "last_number")
    private Long lastNumber;
}