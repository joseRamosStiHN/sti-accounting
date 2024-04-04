package com.sti.accounting.utils;

import com.sti.accounting.models.GeneralError;
import com.sti.accounting.models.GeneralResponse;
import org.springframework.http.HttpStatus;

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

