package com.weightop.controller;

import com.weightop.exception.CommentNotFoundException;
import com.weightop.exception.LikesAlreadyZeroException;
import com.weightop.model.Error;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleInvalidBody(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(e -> "field '" + e.getField() + "' " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Invalid request body: " + details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Error> handleConstraintViolation(ConstraintViolationException ex) {
        String details = ex.getConstraintViolations().stream()
                .map(v -> "parameter '" + lastNodeName(v) + "' " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Invalid request: " + details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Error> handleMethodValidation(HandlerMethodValidationException ex) {
        String details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(e -> "parameter '" + parameterName(result.getMethodParameter()) + "' "
                                + e.getDefaultMessage()))
                .sorted()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Invalid request: " + details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Error> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value '" + ex.getValue() + "' for parameter '"
                + ex.getName() + "': expected " + describeType(ex.getRequiredType()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Error> handleMissingParameter(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Error> handleUnreadableBody(HttpMessageNotReadableException ex) {
        if (ex.getMessage() != null && ex.getMessage().startsWith("Required request body is missing")) {
            return error(HttpStatus.BAD_REQUEST, "Request body is missing");
        }
        if (ex.getMostSpecificCause() instanceof JacksonException jacksonEx && !jacksonEx.getPath().isEmpty()) {
            String field = jacksonEx.getPath().stream()
                    .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            return error(HttpStatus.BAD_REQUEST, "Invalid request body: field '" + field + "' has an invalid value");
        }
        return error(HttpStatus.BAD_REQUEST, "Invalid request body: malformed JSON");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Error> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String supported = ex.getSupportedMethods() == null ? "" : String.join(", ", ex.getSupportedMethods());
        return error(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method " + ex.getMethod()
                + " is not supported for this endpoint. Supported methods: " + supported);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Error> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type '" + ex.getContentType()
                + "' is not supported. Use '" + MediaType.APPLICATION_JSON_VALUE + "'");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Error> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Endpoint " + request.getMethod() + " "
                + request.getRequestURI() + " does not exist");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleUnexpected(Exception ex) {
        log.error("Unexpected error while processing request", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private static String lastNodeName(ConstraintViolation<?> violation) {
        Path.Node last = null;
        for (Path.Node node : violation.getPropertyPath()) {
            last = node;
        }
        return last == null ? violation.getPropertyPath().toString() : last.getName();
    }

    private static String parameterName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.value().isEmpty()) {
            return requestParam.value();
        }
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null && !pathVariable.value().isEmpty()) {
            return pathVariable.value();
        }
        return parameter.getParameterName();
    }

    private static String describeType(Class<?> type) {
        if (type == null) {
            return "a valid value";
        }
        if (type == Long.class || type == Integer.class || type == long.class || type == int.class) {
            return "an integer";
        }
        return "a value of type " + type.getSimpleName();
    }

    private static ResponseEntity<Error> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new Error().error(message));
    }
}
