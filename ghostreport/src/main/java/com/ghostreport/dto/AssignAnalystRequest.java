package com.ghostreport.dto;

import jakarta.validation.constraints.NotNull;

public class AssignAnalystRequest {

    @NotNull
    private Long analystId;

    public AssignAnalystRequest() {
    }

    public Long getAnalystId() {
        return analystId;
    }

    public void setAnalystId(Long analystId) {
        this.analystId = analystId;
    }
}