package com.ghostreport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AssignAnalystRequest {

    @NotNull
    @Positive
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
