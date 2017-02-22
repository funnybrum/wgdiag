package com.brum.wgdiag.logger;

import com.brum.wgdiag.command.diag.DataHandler;
import com.brum.wgdiag.command.diag.Package;

import java.math.BigDecimal;

/**
 * Diagnostic DataHandler for logging purposes.
 */
public class LoggingDiagDataHandler implements DataHandler {
    @Override
    public void handle(String key, String value) {
        // Do nothing.
    }

    @Override
    public void handle(String key, BigDecimal value) {
        DiagDataLogger.addData(key, value);
    }

    @Override
    public void switchPackage(Package pkg) {
        DiagDataLogger.setDiagPackage(pkg);
    }
}
