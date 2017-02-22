package com.brum.wgdiag.command.diag;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Interface for diagnostic commands.
 */
public interface DiagCommand extends com.brum.wgdiag.command.Command {
    /**
     * Parse response and map it to key-value pairs.
     * @param response the response to be parsed and mapped.
     * @return Map of key-value pairs that were extracted from the command response.
     */
    Map<String, String> parseResponse(String response);

    /**
     * Parse response and map it to key-value pairs.
     * @param response the response to be parsed and mapped.
     * @return Map of key-value pairs that were extracted from the command response.
     */
    Map<String, BigDecimal> parseResponseValues(String response);

    /**
     * Get keys for all fields that can be extracted from by command.
     * @return
     */
    List<Field> getDiagFields();
}