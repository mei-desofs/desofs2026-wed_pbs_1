package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyTrackingCodeRequest {

    @NotBlank
    private String trackingCode;

    public VerifyTrackingCodeRequest() {
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }
}