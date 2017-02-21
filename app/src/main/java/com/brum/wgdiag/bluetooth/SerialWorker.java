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
    private Long lastTimeout = 0L;
    private Long nextTimeout = 0L;
    private String lastCommand = null;

    void setResponseListenerEx(ResponseListenerEx listener) {
        this.responseListener = listener;
    }

    SerialWorker(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    void stop() {
        this.stopWorker = true;
        while (this.running) {
            SystemClock.sleep(5);
        }
        if (this.responseListener != null) {
            this.responseListener.onError(new IOException("Stopping bluetooth serial worker..."));
        }
    }

    void restart() {
        this.restart = true;
    }

    private void cleanUp() {
        android.util.Log.d("SW", "Terminating...");
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
        // TODO - temporary patch for testing purposes
        SystemClock.sleep(500);

        while (this.output == null && timeout > 0) {
            SystemClock.sleep(5);
            timeout -= 5;
        }

        if (timeout <= 0) {
            sendResponse("", false);
            return;
        }

        try {
            this.output.write(command.getBytes());
            this.output.write("\n\r".getBytes());
            this.output.flush();
            this.lastCommand = command;
        } catch (IOException ex) {
            if (this.responseListener != null) {
                this.responseListener.onError(ex);
            }
        }
        this.nextTimeout = SystemClock.elapsedRealtime() + timeout;
    }

    @Override
    public void run() {
        running = true;
        StringBuilder data = new StringBuilder();
        try {
            while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                if (this.input == null || this.output == null || this.restart) {
                    initIO();
                }

                int bytesAvailable = 0;
                try {
                    bytesAvailable = this.input.available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        this.input.read(packetBytes);
                        data.append(new String(packetBytes));
                    }
                } catch (IOException ex) {
                    lastTimeout = nextTimeout;
                    if (this.responseListener != null) {
                        this.responseListener.onError(ex);
                    }
                    continue;
                }

                if (bytesAvailable > 0) {
                    String trimmed = data.toString().replace("\n", "").replace("\r", "");
                    if (trimmed.startsWith(this.lastCommand)) {
                        // Some adapters have echo enabled by default and always contain the
                        // command in front of the real response. This cleans it up.
                        trimmed = trimmed.substring(this.lastCommand.length());
                    }
                    if (trimmed.contains("STOPPED")) {
                        // Depending on what the device is doing it may respond with "STOPPED"
                        // indicating that an action is interrupted.
                        trimmed = trimmed.replace("STOPPED", "");
                    }
                    data.setLength(0);
                    data.append(trimmed);
                }

                String current = data.toString();
                if (current.contains(">")) {
                    // Extract all data before the first '>' char and remove it from the
                    // buffer.
                    String response = current.substring(0, current.indexOf('>'));
                    // String post_response = current.substring(current.indexOf('>') + 1);
                    data.setLength(0);
                    // data.append(post_response);

                    lastTimeout = nextTimeout;
                    sendResponse(response, true);
                    continue;
                }

                Long now = SystemClock.elapsedRealtime();
                if (!lastTimeout.equals(nextTimeout) && now > nextTimeout) {
                    lastTimeout = nextTimeout;
                    sendResponse(data.toString(), false);
                    data.setLength(0);
                }

                SystemClock.sleep(20);
            }
        } catch (Exception ex) {
            ResponseListenerEx listener = this.responseListener;
            if (listener != null) {
                listener.onError(ex);
            }
        } finally {
            running = false;
            android.util.Log.d(this.getClass().getSimpleName(), "Worker stopped");
        }
    }

    private void sendResponse(String response, boolean isComplete) {
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
                                  "Got exception for " + response + ": " + ex.toString());
            }
        }
    }


    private void initIO() {
        cleanUp();

        android.util.Log.d("SW", "Starting...");

        try {
            this.device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(this.deviceAddress);
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
            this.socket = this.device.createRfcommSocketToServiceRecord(uuid);
            this.socket.connect();
            this.output = this.socket.getOutputStream();
            this.input = this.socket.getInputStream();
        } catch (Exception ex) {
            ResponseListenerEx listener = this.responseListener;
            if (listener != null) {
                listener.onError(ex);
            }
        }
    }
}