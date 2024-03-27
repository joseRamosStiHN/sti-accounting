package com.sti.accounting.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Account")
@Getter
@Setter
public class AccountEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long Id;

    private String codigo;
    private String descripcion;
    private String tipoEstadoFinanciero;
    private String moneda;
    private String categoria;
    private String tipocuenta;
    private String saldoTipico;
    private boolean hasRegistros;

}
