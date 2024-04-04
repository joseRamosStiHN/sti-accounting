package com.sti.accounting.models;

import lombok.*;

@Getter
@Setter
@ToString
public class GeneralError {

    private String code;

    private String userMessage;

    private String moreInfo;

    private String internalMessage;

}
