package com.ghostreport.dto;

public class ReportResponse {

    private Long id;
    private String description;
    private String category;
    private String status;

    public ReportResponse() {
    }

    public ReportResponse(Long id, String description, String category, String status) {
        this.id = id;
        this.description = description;
        this.category = category;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}