package ua.home.heatingautomation2026.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        String message = ex.getReason() != null
                ? ex.getReason()
                : "Request failed";

        return buildResponse(
                ex.getStatusCode(),
                message,
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError ->
                        errors.putIfAbsent(
                                fieldError.getField(),
                                fieldError.getDefaultMessage()
                        )
                );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidRequestBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid JSON or field value. Check the request body and enum values.",
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid parameter: " + ex.getName(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                 ex.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Database constraint violation at {}",
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "The request conflicts with existing data or database constraints.",
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error while processing {}",
                request.getRequestURI(),
                ex
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred.",
                request.getRequestURI(),
                Map.of()
        );
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatusCode status,
            String message,
            String path,
            Map<String, String> details
    ) {
        HttpStatus resolvedStatus =
                HttpStatus.resolve(status.value());

        String error = resolvedStatus != null
                ? resolvedStatus.getReasonPhrase()
                : "HTTP Error";

        ApiError apiError = new ApiError(
                Instant.now(),
                status.value(),
                error,
                message,
                path,
                details
        );

        return ResponseEntity
                .status(status)
                .body(apiError);
    }
}