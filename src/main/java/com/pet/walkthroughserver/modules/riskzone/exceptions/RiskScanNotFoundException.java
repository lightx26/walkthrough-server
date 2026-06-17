package com.pet.walkthroughserver.modules.riskzone.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class RiskScanNotFoundException extends AppException {
    public RiskScanNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RISK_SCAN_NOT_FOUND", message);
    }
}
