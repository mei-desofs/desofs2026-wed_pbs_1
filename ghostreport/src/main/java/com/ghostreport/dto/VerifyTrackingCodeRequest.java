package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyTrackingCodeRequest {

    @NotBlank(message = "Tracking code is required")
    private String trackingCode;

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }
}