package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import static com.ghostreport.validation.ValidationConstants.TRACKING_CODE_PATTERN;

public class DownloadRequest {

    @NotBlank
    @Size(max = 67)
    @Pattern(regexp = TRACKING_CODE_PATTERN)
    private String trackingCode;

    @NotNull
    @Positive
    private Long attachmentId;

    public DownloadRequest() {
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Long attachmentId) {
        this.attachmentId = attachmentId;
    }
}
