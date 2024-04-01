package com.sti.accounting.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cuentas")
@NoArgsConstructor
@AllArgsConstructor
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


    //constructors
    public AccountEntity(Long id){
        this.Id = id;
    }
}
