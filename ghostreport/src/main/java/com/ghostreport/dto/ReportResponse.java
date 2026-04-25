package com.ghostreport.dto;

public class ReportResponse {

    private Long id;
    private String status;
    private String category;
    private String description;

    public ReportResponse(Long id, String status, String category, String description) {
        this.id = id;
        this.status = status;
        this.category = category;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getStatus() { return status; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
}