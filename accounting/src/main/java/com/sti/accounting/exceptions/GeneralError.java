package com.sti.accounting.exceptions;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class GeneralError {
    private int code;
    private String message;
}
