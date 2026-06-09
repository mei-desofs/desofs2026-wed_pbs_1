package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static com.ghostreport.validation.ValidationConstants.CASE_PRIORITY_ALLOWLIST;

public class UpdatePriorityRequest {

    @NotBlank
    @Pattern(regexp = CASE_PRIORITY_ALLOWLIST, flags = Pattern.Flag.CASE_INSENSITIVE)
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
