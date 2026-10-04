package com.weightop.controller;

import com.weightop.exception.CommentNotFoundException;
import com.weightop.exception.LikesAlreadyZeroException;
import com.weightop.model.Error;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<Error> handleNotFound(CommentNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(LikesAlreadyZeroException.class)
    public ResponseEntity<Error> handleLikesAlreadyZero(LikesAlreadyZeroException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Error> handleBadRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    private static ResponseEntity<Error> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new Error().error(message));
    }
}
