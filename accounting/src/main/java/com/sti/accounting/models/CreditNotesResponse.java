package com.sti.accounting.models;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class CreditNotesResponse {

    private Long id;

    private Long transactionId;

    private String reference;

    private String descriptionNote;

    private String invoiceNo;

    private String numberPda;

    private Long diaryType;

    private String diaryName;

    private String status;

    private LocalDateTime creationDate;

    private LocalDate date;

    private String user;

    private Set<CreditNotesDetailResponse> detailNote;
}
