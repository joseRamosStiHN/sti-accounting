package com.sti.accounting.exceptions;

import lombok.Getter;



@Getter
enum ErrorResponseCode {
    PARAMETER_REQUIRED(-1, "Field %s is required"),
    UNKNOWN_ERROR(-10, "We got and error while processing the request, please try again");

    ErrorResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private final int code;
    private final String message;

}
