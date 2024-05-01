package com.sti.accounting.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        logger.info("MethodArgumentTypeMismatchException: {}", ex.getMessage());

        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logger.info("handleHttpMessageNotReadable: {}", ex.getLocalizedMessage());

        return new ResponseEntity<>(ex, HttpStatus.BAD_REQUEST);

    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logger.info("handleMethodArgumentNotValid: {}", ex.getLocalizedMessage());
        List<GeneralError> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(er ->{
            GeneralError error = new GeneralError();
            error.setMessage(String.format(ErrorResponseCode.PARAMETER_REQUIRED.getMessage(), er.getField()));
            error.setCode(ErrorResponseCode.PARAMETER_REQUIRED.getCode());
            errors.add(error);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        logger.error("handleResponseStatusException:", ex);
        List<GeneralError> responseError = new ArrayList<>();
        GeneralError error = new GeneralError();
        error.setMessage(ex.getReason());
        error.setCode(ex.getStatusCode().value());
        responseError.add(error);
        return new ResponseEntity<>(responseError, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        logger.info("Unhandled exception:", ex);
        List<GeneralError> errors = new ArrayList<>();
        GeneralError error = new GeneralError();
        error.setMessage(ErrorResponseCode.UNKNOWN_ERROR.getMessage());
        error.setCode(ErrorResponseCode.UNKNOWN_ERROR.getCode());
        errors.add(error);

        return new ResponseEntity<>(errors, HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
