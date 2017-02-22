package com.brum.wgdiag.command.diag;

import java.math.BigDecimal;

/**
 * Handler for diag data values. Called every time a value is being decoded.
 */
public interface DataHandler {

    /**
     * Called when a value has been decoded.
     *
     * @param key diagnostic value key
     * @param value diagnostic value
     */
    void handle(String key, String value);

    /**
     * Called when a value has been decoded.
     *
     * @param key diagnostic value key
     * @param value diagnostic value
     */
    void handle(String key, BigDecimal value);

    /**
     * Called every time a diagnostic data package is being switched.
     * @param pkg
     */
    void switchPackage(Package pkg);
}
