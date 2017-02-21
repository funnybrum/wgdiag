package com.brum.wgdiag.bluetooth;

/**
 * Response listener for diag command results.
 */
public interface ResponseListener {

    void onResponse(String response);

    void onIncompleteResponse(String response);
}
