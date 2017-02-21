package com.brum.wgdiag.activity.utils;

/**
 * Allows interruption of endless activity, like the diagnostic command package execution.
 */
public interface ExecutionInterrupter {

    /**
     * Break the execution .
     */
    void interrupt(boolean block);
}
