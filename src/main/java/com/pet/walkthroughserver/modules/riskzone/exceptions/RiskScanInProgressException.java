package com.pet.walkthroughserver.modules.riskzone.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class RiskScanInProgressException extends AppException {
    public RiskScanInProgressException(String message) {
        super(HttpStatus.CONFLICT, "RISK_SCAN_IN_PROGRESS", message);
    }
}
