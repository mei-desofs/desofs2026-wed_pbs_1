package com.ghostreport.dto;

public class CreateReportResponse {

    private Long id;
    private String status;
    private String trackingCode;

    public CreateReportResponse(Long id, String status, String trackingCode) {
        this.id = id;
        this.status = status;
        this.trackingCode = trackingCode;
    }

    public Long getId() { return id; }
    public String getStatus() { return status; }
    public String getTrackingCode() { return trackingCode; }
}