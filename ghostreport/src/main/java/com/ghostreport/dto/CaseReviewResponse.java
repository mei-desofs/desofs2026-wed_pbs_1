package com.ghostreport.dto;

public class CaseReviewResponse {

    private Long reportId;
    private Long caseReviewId;
    private String assignedAnalystUsername;
    private String priority;
    private String notes;
    private String reportStatus;

    public CaseReviewResponse() {
    }

    public CaseReviewResponse(Long reportId, Long caseReviewId, String assignedAnalystUsername, String priority, String notes, String reportStatus) {
        this.reportId = reportId;
        this.caseReviewId = caseReviewId;
        this.assignedAnalystUsername = assignedAnalystUsername;
        this.priority = priority;
        this.notes = notes;
        this.reportStatus = reportStatus;
    }

    public Long getReportId() {
        return reportId;
    }

    public Long getCaseReviewId() {
        return caseReviewId;
    }

    public String getAssignedAnalystUsername() {
        return assignedAnalystUsername;
    }

    public String getPriority() {
        return priority;
    }

    public String getNotes() {
        return notes;
    }

    public String getReportStatus() {
        return reportStatus;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public void setCaseReviewId(Long caseReviewId) {
        this.caseReviewId = caseReviewId;
    }

    public void setAssignedAnalystUsername(String assignedAnalystUsername) {
        this.assignedAnalystUsername = assignedAnalystUsername;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setReportStatus(String reportStatus) {
        this.reportStatus = reportStatus;
    }
}