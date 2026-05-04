package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateReportRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 4000)
    private String description;

    @NotBlank
    @Size(max = 100)
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