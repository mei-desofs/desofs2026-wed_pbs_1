package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MfaVerifyRequest {

    @NotBlank
    @Size(max = 80)
    private String challengeId;

    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String code;

    public MfaVerifyRequest() {
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
