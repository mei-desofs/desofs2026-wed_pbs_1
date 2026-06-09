package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static com.ghostreport.validation.ValidationConstants.REPORT_STATUS_ALLOWLIST;

public class UpdateReportStatusRequest {

    @NotBlank
    @Pattern(regexp = REPORT_STATUS_ALLOWLIST, flags = Pattern.Flag.CASE_INSENSITIVE)
    private String status;

    public UpdateReportStatusRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
