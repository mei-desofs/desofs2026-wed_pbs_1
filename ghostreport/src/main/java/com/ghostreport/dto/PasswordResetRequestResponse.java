package com.ghostreport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PasswordResetRequestResponse(String message, String resetToken) {
}
