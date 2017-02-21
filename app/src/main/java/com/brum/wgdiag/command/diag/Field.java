package com.brum.wgdiag.command.diag;

import java.math.BigDecimal;

/**
 * Represent a single diagnostic field.
 */
public interface Field {
    /**
     * Field key. All equal fields should have equal key. No different fields should have the same key.
     * @return field key.
     */
    String getKey();

    /**
     * Field description.
     * @return
     */
    String getDescription();

    /**
     * Parse field value as big decimal.
     * @param response
     * @return decimal representing the field value.
     */
    BigDecimal toDecimal(String response);

    /**
     * Parse field value as string (including the units applicable for it).
     * @param response
     * @return string representation of the field value.
     */
    String toString(String response);
}

