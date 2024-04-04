package com.sti.accounting.utils;

import com.sti.accounting.models.GeneralError;
import com.sti.accounting.models.GeneralResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

public class Util {

    public GeneralResponse setSuccessResponse(Object data, HttpStatus status) {
        GeneralResponse gr = new GeneralResponse();
        gr.setCode((long) status.value());
        gr.setDescription("Operation Successful");
        if (data != null) {
            gr.setData(data);
        }
        return gr;
    }


    public GeneralResponse setSuccessWithoutData() {
        GeneralResponse gr = new GeneralResponse();
        gr.setCode(1L);
        gr.setDescription("Operation Successful");
        return gr;
    }

    public GeneralResponse setValidationError(BindingResult bindingResult) {
        GeneralResponse gr = new GeneralResponse();
        gr.setCode((long) HttpStatus.BAD_REQUEST.value());
        gr.setDescription("Validation Error");

        bindingResult.getFieldErrors().forEach(error -> {
            GeneralError validationError = new GeneralError();
            validationError.setCode(String.valueOf(HttpStatus.BAD_REQUEST.value()));
            validationError.setUserMessage(error.getDefaultMessage());
            validationError.setMoreInfo("Validation Error");
            validationError.setInternalMessage(error.getField());
            gr.getErrors().add(validationError);
        });

        return gr;
    }

    public GeneralResponse setError(HttpStatus status, String userMessage, String moreInfo) {
        GeneralResponse gr = new GeneralResponse();
        GeneralError error = new GeneralError();
        error.setCode(String.valueOf(status.value()));
        error.setUserMessage(userMessage);
        error.setMoreInfo(moreInfo);
        error.setInternalMessage("check the log for more information");
        gr.setCode((long) status.value());
        gr.setDescription(status.getReasonPhrase());
        gr.getErrors().add(error);
        return gr;
    }


}

