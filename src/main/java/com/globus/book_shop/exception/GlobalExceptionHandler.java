package com.globus.book_shop.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFoundException(
            BookNotFoundException ex, WebRequest request) {
        String path = extractPath(request);
        log.warn("Book not found: {} - path: {}", ex.getMessage(), path);
        return buildErrorResponse(
                "The requested book was not found",
                "Book Not Found",
                HttpStatus.NOT_FOUND,
                path,
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(BookPriceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookPriceNotFoundException(
            BookPriceNotFoundException ex, WebRequest request) {
        String path = extractPath(request);
        log.warn("Book price not found: {} - path: {}", ex.getMessage(), path);
        return buildErrorResponse(
                "The price information for the requested book was not found",
                "Book Price Not Found",
                HttpStatus.NOT_FOUND,
                path,
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(CurrencyServiceException.class)
    public ResponseEntity<ErrorResponse> handleCurrencyServiceException(
            CurrencyServiceException ex, WebRequest request) {
        String path = extractPath(request);
        log.error("Currency service error: {} - path: {}", ex.getMessage(), path, ex);
        return buildErrorResponse(
                "Unable to fetch currency exchange rates at this time",
                "Currency Service Error",
                HttpStatus.SERVICE_UNAVAILABLE,
                path,
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        String path = extractPath(request);
        Map<String, List<String>> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        error -> error.getField(),
                        Collectors.mapping(
                                error -> error.getDefaultMessage(),
                                Collectors.toList()
                        )
                ));

        int totalErrors = validationErrors.values().stream()
                .mapToInt(List::size)
                .sum();

        log.warn("Validation failed: {} errors in {} fields - path: {}", 
                totalErrors, validationErrors.size(), path);
        return buildErrorResponse(
                "The request contains invalid data. Please check the validation errors below",
                "Validation Failed",
                HttpStatus.BAD_REQUEST,
                path,
                String.format("Validation failed for %d field(s) with %d error(s)", 
                        validationErrors.size(), totalErrors),
                validationErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        String path = extractPath(request);
        Map<String, List<String>> validationErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(
                        violation -> {
                            String violationPath = violation.getPropertyPath().toString();
                            int lastDot = violationPath.lastIndexOf('.');
                            return lastDot >= 0 ? violationPath.substring(lastDot + 1) : violationPath;
                        },
                        Collectors.mapping(
                                violation -> violation.getMessage(),
                                Collectors.toList()
                        )
                ));

        int totalErrors = validationErrors.values().stream()
                .mapToInt(List::size)
                .sum();

        log.warn("Constraint violation: {} errors in {} fields - path: {}", 
                totalErrors, validationErrors.size(), path);
        return buildErrorResponse(
                "The request parameters contain invalid values. Please check the validation errors below",
                "Validation Failed",
                HttpStatus.BAD_REQUEST,
                path,
                String.format("Validation failed for %d parameter(s) with %d error(s)", 
                        validationErrors.size(), totalErrors),
                validationErrors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        String path = extractPath(request);
        log.error("Unexpected error - path: {}", path, ex);
        return buildErrorResponse(
                "An unexpected error occurred while processing your request",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                path,
                "Please try again later or contact support if the problem persists",
                null
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            String message, String error, HttpStatus status, String path,
            String details, Map<String, List<String>> validationErrors) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .details(details)
                .validationErrors(validationErrors)
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}