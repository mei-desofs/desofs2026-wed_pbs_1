package com.ghostreport.service;

import com.ghostreport.model.ReportStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class ReportWorkflowPolicy {

    private static final Map<ReportStatus, Set<ReportStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ReportStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
                ReportStatus.SUBMITTED,
                EnumSet.of(ReportStatus.UNDER_REVIEW, ReportStatus.REJECTED)
        );
        ALLOWED_TRANSITIONS.put(
                ReportStatus.UNDER_REVIEW,
                EnumSet.of(ReportStatus.MORE_INFO_REQUIRED, ReportStatus.RESOLVED, ReportStatus.REJECTED)
        );
        ALLOWED_TRANSITIONS.put(
                ReportStatus.MORE_INFO_REQUIRED,
                EnumSet.of(ReportStatus.UNDER_REVIEW, ReportStatus.RESOLVED, ReportStatus.REJECTED)
        );
        ALLOWED_TRANSITIONS.put(ReportStatus.RESOLVED, EnumSet.noneOf(ReportStatus.class));
        ALLOWED_TRANSITIONS.put(ReportStatus.REJECTED, EnumSet.noneOf(ReportStatus.class));
    }

    public void validateTransition(ReportStatus currentStatus, ReportStatus requestedStatus) {
        if (currentStatus == requestedStatus) {
            return;
        }

        Set<ReportStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(
                currentStatus,
                EnumSet.noneOf(ReportStatus.class)
        );

        if (!allowedTargets.contains(requestedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid report status transition from %s to %s".formatted(currentStatus, requestedStatus)
            );
        }
    }
}
