package com.ghostreport.dto;

import jakarta.validation.constraints.NotBlank;

public class PasswordResetRequest {

    @NotBlank
    private String usernameOrEmail;

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }
}
