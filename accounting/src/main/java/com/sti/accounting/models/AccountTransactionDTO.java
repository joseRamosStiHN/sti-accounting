package com.sti.accounting.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountTransactionDTO {
    private String description;
    private String code;
    private String fatherAccount;
    private String date;
    private String typeMovement;
    private String motion;
    private String amount;
    private String numberPda;
    private String categoryName;
}
