package com.ecommerce.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for error responses.
 * <p>
 * Standard error response format for API contract compliance.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** Error type code */
    private String error;

    /** Human-readable error message */
    private String message;

    /** HTTP status code */
    private int status;
}
