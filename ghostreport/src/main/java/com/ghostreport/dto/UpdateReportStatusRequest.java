package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateReportStatusRequest {

    @NotBlank
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