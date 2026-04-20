package com.ghostreport.dto;

public class AttachmentResponse {

    private Long id;
    private String originalName;
    private String mimeType;
    private Long size;

    public AttachmentResponse() {
    }

    public AttachmentResponse(Long id, String originalName, String mimeType, Long size) {
        this.id = id;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.size = size;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getSize() {
        return size;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}