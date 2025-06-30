package com.jgy36.PoliticalApp.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ Utility method to format error responses
    private static ErrorResponse createErrorResponse(String error, String message) {
        return new ErrorResponse(error, message);
    }

    // ✅ Handle authentication errors (401)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                createErrorResponse("Unauthorized", ex.getMessage())
        );
    }

    // ✅ Handle access denied (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                createErrorResponse("Forbidden", ex.getMessage())
        );
    }

    // ✅ Handle not found errors (404)
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                createErrorResponse("Not Found", ex.getMessage())
        );
    }

    // ✅ Handle generic bad requests (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                createErrorResponse("Bad Request", ex.getMessage())
        );
    }

    // ✅ Handle JSON serialization errors - KEEP THIS ONE, REMOVE THE DUPLICATE
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonSerializationException(HttpMessageNotWritableException ex) {
        System.err.println("JSON Serialization error: " + ex.getMessage());
        ex.printStackTrace();

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Unable to serialize response to JSON");
        response.put("message", ex.getMessage());

        // Return a simple, serializable response
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ✅ Fallback for unexpected errors (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                createErrorResponse("Internal Server Error", ex.getMessage())
        );
    }

    // ✅ Inner class for consistent error response format
    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    }
}
