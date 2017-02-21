package com.brum.wgdiag.bluetooth;

/**
 * Response listener for diag command results with exception handling.
 */
public interface ResponseListenerEx extends ResponseListener {

    public void onError(Exception ex);

}
