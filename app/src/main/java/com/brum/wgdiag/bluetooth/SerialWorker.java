package com.brum.wgdiag.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Serial worker. Takes care for async bluetooth serial interface communication.
 */
class SerialWorker implements Runnable {
    private final String deviceAddress;
    private ResponseListenerEx responseListener = null;

    private BluetoothDevice device = null;
    private BluetoothSocket socket = null;
    private OutputStream output = null;
    private InputStream input = null;

    private boolean stopWorker = false;
    private boolean restart = false;
    private boolean running = false;
    private Long nextTimeout = 0L;
    private String currentCommand = null;

    SerialWorker(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    void stop() {
        this.stopWorker = true;
        while (this.running) {
            SystemClock.sleep(5);
        }
        respond(new IOException("Stopping bluetooth serial worker..."));
    }

    void restart() {
        this.restart = true;
    }

    private void cleanUp() {
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                // Ignore.
            } finally {
                this.socket = null;
            }
        }

        if (this.input != null) {
            try {
                this.input.close();
            } catch (IOException ex) {
                // ignore.
            } finally {
                this.input = null;
            }
        }

        if (this.output != null) {
            try {
                this.output.close();
            } catch (IOException ex) {
                // Ignore.
            } finally {
                this.output = null;
            }
        }
        this.restart = false;
    }

    void sendCommand(String command, Long timeout) {
        while (this.output == null && timeout > 0) {
            SystemClock.sleep(5);
            timeout -= 5;
        }

        while (this.currentCommand != null && timeout > 0) {
            SystemClock.sleep(5);
            timeout -= 5;
        }

        if (timeout <= 0) {
            throw new RuntimeException("Failed to send command...");
        }

        try {
            this.currentCommand = command;
            this.output.write(command.getBytes());
            this.output.write("\n\r".getBytes());
            this.output.flush();
        } catch (IOException ex) {
            respond(ex);
        }
        this.nextTimeout = SystemClock.elapsedRealtime() + timeout;
    }

    void setResponseListenerEx(ResponseListenerEx listener) {
        long timeout = 10000;
        while (this.currentCommand != null && timeout > 0) {
            SystemClock.sleep(5);
            timeout -= 5;
        }

        if (timeout == 0) {
            throw new RuntimeException("Failed to complete command");
        }

        this.responseListener = listener;
    }

    @Override
    public void run() {
        android.util.Log.d("SW", "Starting...");
        running = true;
        StringBuilder data = new StringBuilder();
        try {
            while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                if (this.input == null || this.output == null || this.restart) {
                    initIO();
                }

                int bytesAvailable = this.input.available();
                if (bytesAvailable > 0) {
                    byte[] packetBytes = new byte[bytesAvailable];
                    this.input.read(packetBytes);
                    data.append(new String(packetBytes));
                }

                if (bytesAvailable > 0) {
                    String trimmed = data.toString().replace("\n", "").replace("\r", "");
                    if (trimmed.startsWith(this.currentCommand)) {
                        // Some adapters have echo enabled by default and always contain the
                        // command in front of the real response. This cleans it up.
                        trimmed = trimmed.substring(this.currentCommand.length());
                    }
                    if (trimmed.contains("STOPPED")) {
                        // Depending on what the device is doing it may respond with "STOPPED"
                        // indicating that an action is interrupted. This doesn't bring any
                        // useful information for our use case. So - ignore it.
                        trimmed = trimmed.replace("STOPPED", "");
                    }
                    data.setLength(0);
                    data.append(trimmed);
                }

                String current = data.toString();
                if (current.contains(">")) {
                    // Extract all data before the first '>' char and use it as response.
                    String response = current.substring(0, current.indexOf('>'));
                    data.setLength(0);

                    respond(response, true);
                    continue;
                }

                Long now = SystemClock.elapsedRealtime();
                if (currentCommand != null && now > nextTimeout) {
                    respond(data.toString(), false);
                    data.setLength(0);
                }

                SystemClock.sleep(20);
            }
        } catch (Exception ex) {
            respond(ex);
        } finally {
            running = false;
            android.util.Log.d(this.getClass().getSimpleName(), "Worker stopped");
        }
    }

    /**
     * Send the specified response to the listener if there is a command currently executing.
     * @param response
     * @param isComplete
     */
    private void respond(String response, boolean isComplete) {
        if (this.currentCommand == null) {
            // No command was send, no need for posting a replay.
            return;
        }

        this.currentCommand = null;
        if (this.responseListener != null) {
            response = response.replace("\n", "").replace("\r", "");
            try {
                if (isComplete) {
                    responseListener.onResponse(response);
                } else {
                    responseListener.onIncompleteResponse(response);
                }
            } catch (RuntimeException ex) {
                android.util.Log.d(this.getClass().getSimpleName(),
                                  "Got exception for " + response + " -> " + ex.toString());
            }
        }
    }

    /**
     * Send an error response to the current listener if there is a command currently executing.
     * @param error the error to be send.
     */
    private void respond(Exception error) {
        if (this.currentCommand == null) {
            // No command was send, no need for posting an error.
            return;
        }

        this.currentCommand = null;
        if (this.responseListener != null) {
            try {
                responseListener.onError(error);
            } catch (RuntimeException ex) {
                android.util.Log.d(this.getClass().getSimpleName(),
                        "Got exception on .onError() -> " + ex.toString());
            }
        }
    }


    private void initIO() {
        cleanUp();

        android.util.Log.d("SW", "Initializing...");

        try {
            this.device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(this.deviceAddress);
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
            this.socket = this.device.createRfcommSocketToServiceRecord(uuid);
            this.socket.connect();
            this.output = this.socket.getOutputStream();
            this.input = this.socket.getInputStream();
            android.util.Log.d("SW", "Initialization completed.");
        } catch (Exception ex) {
            android.util.Log.d("SW", "Failed to initialize: " + ex.getMessage());
            respond(ex);
        }
    }
}