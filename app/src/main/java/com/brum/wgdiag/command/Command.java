package com.brum.wgdiag.command;

/**
 * Interface for basic command.
 */
public interface Command {
    /**
     * DiagCommand string to be send.
     */
    String getRequestCommand();

    /**
     * Verifies the response of the command.
     * @param response the response string to be verified.
     * @return true iff the command response is correct.
     */
    boolean verifyResponse(String response);

    /**
     * Get command timeout.
     * @return timeout.
     */
    long getTimeoutMillis();
}
