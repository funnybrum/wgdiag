package com.brum.wgdiag.bluetooth;

import android.util.Log;

import com.brum.wgdiag.command.Command;
import com.brum.wgdiag.util.Executor;

/**
 * Bluetooth service code. Provide static initialized interface for sending commands and receiving
 * responses. Initialize the bluetooth serial connection once .init() is invoked and keep it open
 * until .stop() is invoked.
 */
public class Service {
    private static SerialWorker worker = null;

    /**
     * Initialize the bluetooth serial communicator service.
     * @param deviceAddress the device addressed that should be connected.
     * @throws IllegalStateException if already started.
     */
    public static void init(String deviceAddress) {
        if (Service.worker != null && Service.worker.isRunning()) {
            return;
        }

        if (Service.worker == null) {
            Service.worker = new SerialWorker(deviceAddress);
        }

        Executor.execute(Service.worker);
    }

    public static void stop() {
        if (Service.worker == null) {
            return;
        }

        worker.stop();

    }

    /**
     * Set the response listener. This listener will receive the next response.
     *
     * @param listener the listener.
     * @throws IllegalStateException if not started.
     */
    public static void setResponseListener(final ResponseListener listener) {
        Service.worker.setResponseListenerEx(new ResponseListenerEx() {
            @Override
            public void onResponse(String response) {
                Log.d(Service.class.getSimpleName(), " < \"" + response + "\"");
                listener.onResponse(response);
            }

            @Override
            public void onIncompleteResponse(String response) {
                Log.d(Service.class.getSimpleName(), " (timeout) < \"" + response + "\"");
                listener.onIncompleteResponse(response);
            }

            @Override
            public void onError(Exception ex) {
                Log.d(Service.class.getSimpleName(), "Got error, restarting.", ex);
                Service.worker.restart();
                if (listener instanceof ResponseListenerEx) {
                    ((ResponseListenerEx)listener).onError(ex);
                } else {
                    listener.onIncompleteResponse("");
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
        Service.worker.sendCommand(cmd.getRequestCommand(), cmd.getTimeoutMillis());
        Log.d(Service.class.getSimpleName(), " > \"" + cmd.getRequestCommand() + "\"");
    }
}
