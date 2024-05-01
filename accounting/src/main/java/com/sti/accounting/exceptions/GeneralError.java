package com.sti.accounting.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneralError {
    private int code;
    private String message;
}
