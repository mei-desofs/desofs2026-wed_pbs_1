package com.ghostreport.dto;

public class AttachmentSummaryResponse {

    private long attachmentCount;

    public AttachmentSummaryResponse() {
    }

    public AttachmentSummaryResponse(long attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public long getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(long attachmentCount) {
        this.attachmentCount = attachmentCount;
    }
}
