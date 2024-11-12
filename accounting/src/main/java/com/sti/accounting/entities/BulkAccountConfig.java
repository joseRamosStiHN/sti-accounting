package com.sti.accounting.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Entity
@Table(name = "bulk_account_config")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkAccountConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", columnDefinition = "VARCHAR(255)")
    private String name;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "type")
    private Long type; // venta / compra

    @Column(name = "row_start")
    private Integer rowStart;

    @Column(name = "tenant_id")
    private String tenantId;

    @OneToMany
    @JoinColumn(name = "bulk_account_config_id", referencedColumnName = "id")
    private List<BulkAccountConfigDetail> details;

}
