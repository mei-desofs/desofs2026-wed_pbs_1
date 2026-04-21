package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdatePriorityRequest {

    @NotBlank
    private String priority;

    public UpdatePriorityRequest() {
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}