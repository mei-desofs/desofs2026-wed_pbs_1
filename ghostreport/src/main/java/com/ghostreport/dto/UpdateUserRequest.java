package com.ghostreport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.ghostreport.validation.ValidationConstants.USERNAME_PATTERN;
import static com.ghostreport.validation.ValidationConstants.USER_ROLE_ALLOWLIST;

public class UpdateUserRequest {

    @NotBlank
    @Size(min = 3, max = 120)
    @Pattern(regexp = USERNAME_PATTERN)
    private String username;

    @Email
    @NotBlank
    @Size(max = 160)
    private String email;

    @NotBlank
    @Pattern(regexp = USER_ROLE_ALLOWLIST, flags = Pattern.Flag.CASE_INSENSITIVE)
    private String role;

    @NotNull
    private Boolean active;

    public UpdateUserRequest() {
        // Required by Jackson for request-body binding before Jakarta Bean Validation runs.
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
