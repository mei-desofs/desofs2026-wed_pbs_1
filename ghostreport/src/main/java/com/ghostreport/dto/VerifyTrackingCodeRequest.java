package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.ghostreport.validation.ValidationConstants.TRACKING_CODE_PATTERN;

public class VerifyTrackingCodeRequest {

    @NotBlank
    @Size(max = 67)
    @Pattern(regexp = TRACKING_CODE_PATTERN)
    private String trackingCode;

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }
}
