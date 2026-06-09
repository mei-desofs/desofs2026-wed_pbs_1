package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.ghostreport.validation.ValidationConstants.REPORT_CATEGORY_ALLOWLIST;

public class CreateReportRequest {

    @NotBlank
    @Size(min = 3, max = 200)
    private String title;

    @NotBlank
    @Size(min = 10, max = 3000)
    private String description;

    @NotBlank
    @Size(max = 40)
    @Pattern(regexp = REPORT_CATEGORY_ALLOWLIST)
    private String category;

    public CreateReportRequest() {}

    // GETTERS
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }

    // SETTERS
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
}
