package com.sti.accounting.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AccountTransactionDTO {
    private String description;
    private String code;
    private String cuentaPadre;
    private String date;
    private String movimiento;
    private String motion;
    private String amount;
    private String numberPda;
    private String categoryName;


    public AccountTransactionDTO(String description, String code, String cuentaPadre, String date, String movimiento, String motion, String amount, String numberPda, String categoryName) {
        this.description = description;
        this.code = code;
        this.cuentaPadre = cuentaPadre;
        this.date = date;
        this.movimiento = movimiento;
        this.motion = motion;
        this.amount = amount;
        this.numberPda = numberPda;
        this.categoryName = categoryName;
    }
}
