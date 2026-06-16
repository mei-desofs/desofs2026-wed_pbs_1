package com.ghostreport.dto;

public class ReportResponse {

    private Long id;
    private String title;
    private String status;
    private String category;
    private String description;
    private long attachmentCount;

    public ReportResponse(Long id, String title, String status, String category, String description) {
        this(id, title, status, category, description, 0);
    }

    public ReportResponse(Long id, String title, String status, String category, String description, long attachmentCount) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.category = category;
        this.description = description;
        this.attachmentCount = attachmentCount;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public long getAttachmentCount() { return attachmentCount; }
}
