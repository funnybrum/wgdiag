package com.brum.wgdiag.bluetooth;

import com.brum.wgdiag.command.Command;

/**
 * Bluetooth service code. Provide static initialized interface for sending commands and receiving
 * responses. Initialize the bluetooth serial connection once .start() is invoked and keep it open
 * until .stop() is invoked.
 */
public class Service {
    private static SerialWorker worker = null;

    /**
     * Start the bluetooth serial communicator service.
     * @param deviceAddress the device addressed that should be connected.
     * @throws IllegalStateException if already started.
     */
    public static void start(String deviceAddress) {
        synchronized (Service.class) {
            if (Service.worker != null) {
                throw new IllegalStateException("Already initialized.");
            }

            Service.worker = new SerialWorker(deviceAddress);
            new Thread(Service.worker).start();
        }
    }

    /**
     * Stop the bluetooth serial communicator service and release the resources.
     */
    public static void stop() {
        synchronized (Service.class) {
            if (Service.worker != null) {
                Service.worker.stop();
            }

            Service.worker = null;
        }
    }

    /**
     * Set the response listener. This listener will receive the next response.
     *
     * @param listener the listener.
     * @throws IllegalStateException if not started.
     */
    public static void setResponseListener(final ResponseListener listener) {
        verifyInitialized();
        Service.worker.setResponseListenerEx(new ResponseListenerEx() {
            @Override
            public void onResponse(String response) {
                android.util.Log.d("BTSVC < ", response);
                listener.onResponse(response);
            }

            @Override
            public void onIncompleteResponse(String response) {
                android.util.Log.d("BTSVC (timeout) < ", response);
                listener.onIncompleteResponse(response);
            }

            @Override
            public void onError(Exception ex) {
                android.util.Log.d("BTSVC", "Got error, restarting: " + ex.getMessage());
                Service.worker.restart();
                if (listener instanceof ResponseListenerEx) {
                    ((ResponseListenerEx)listener).onError(ex);
                }
            }
        });
    }

    /**
     * Send the specified command over the bluetooth serial connection.
     * @param cmd the command.
     * @throws IllegalStateException if not started.
     */
    public static void write(Command cmd) {
        write(cmd.getRequestCommand(), cmd.getTimeoutMillis());
    }

    /**
     * Send the specified command with timeout of 2 seconds.
     * @param command the request command.
     * @throws IllegalStateException if not started.
     */
    public static void write(String command) {
        write(command, 2 * 1000L);
    }

    /**
     * Sends the specified command over the bluetooth serial connection.
     *
     * @param command the request command.
     * @param timeoutMillis the timeout (milliseconds).
     * @throws IllegalStateException if not started.
     */
    public static void write(String command, long timeoutMillis) {
        verifyInitialized();
        android.util.Log.d("BTSVC > ", command);
        Service.worker.sendCommand(command, timeoutMillis);
    }

    private static void verifyInitialized() {
        if (Service.worker == null) {
            throw new IllegalStateException("Not initialized!");
        }
    }
}
