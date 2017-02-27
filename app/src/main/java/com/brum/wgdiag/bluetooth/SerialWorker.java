package com.brum.wgdiag.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import android.util.Log;

import com.brum.wgdiag.util.Executor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
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
        cleanUp();
        respond(new IOException("Stopping bluetooth serial worker..."));
    }

    void restart() {
        this.restart = true;
    }

    private void cleanUp() {
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException ex) {
                Log.d(this.getClass().getSimpleName(), "Got ignored exception", ex);
            } finally {
                this.socket = null;
            }
        }

        if (this.input != null) {
            try {
                this.input.close();
            } catch (IOException ex) {
                Log.d(this.getClass().getSimpleName(), "Got ignored exception", ex);
            } finally {
                this.input = null;
            }
        }

        if (this.output != null) {
            try {
                this.output.close();
            } catch (IOException ex) {
                Log.d(this.getClass().getSimpleName(), "Got ignored exception", ex);
            } finally {
                this.output = null;
            }
        }
        this.restart = false;
    }

    /**
     * Write the provided command over the bluetooth serial output stream.
     *
     * The command is written in non-blocking manner in another thread.
     *
     * If there is a command that is being executed currently - this command will be blocked until
     * that command is completed/timed out.
     * @param command the command to be written.
     * @param timeout the timeout in milliseconds for the command to be executed.
     */
    void sendCommand(final String command, final Long timeout) {
        Executor.execute(new Runnable() {
            @Override
            public void run() {
                long sendCommandTimeout = timeout;
                while (SerialWorker.this.output == null && sendCommandTimeout > 0) {
                    SystemClock.sleep(5);
                    sendCommandTimeout -= 5;
                }

                while (SerialWorker.this.currentCommand != null && sendCommandTimeout > 0) {
                    SystemClock.sleep(5);
                    sendCommandTimeout -= 5;
                }

                if (sendCommandTimeout <= 0) {
                    // We should not be getting here. If we do - there is no way currently to handle
                    // this - so just throw an exception.
                    SerialWorker.this.currentCommand = command;
                    respond(new RuntimeException("Failed to send command..."));
                    return;
                }

                try {
                    SerialWorker.this.currentCommand = command;
                    SerialWorker.this.output.write(command.getBytes());
                    SerialWorker.this.output.write("\n\r".getBytes());
                    SerialWorker.this.output.flush();
                } catch (IOException ex) {
                    respond(ex);
                }
                SerialWorker.this.nextTimeout = SystemClock.elapsedRealtime() + timeout;
            }
        });
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
        Log.d(this.getClass().getSimpleName(), "Starting...");
        running = true;
        StringBuilder buffer = new StringBuilder();
        try {
            while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                // Check if we've hit a timeout
                Long now = SystemClock.elapsedRealtime();
                if (currentCommand != null && now > nextTimeout) {
                    respond(buffer.toString(), false);
                    buffer.setLength(0);
                }

                // Check if the serial IO is initialized.
                if (this.input == null || this.output == null || this.restart) {
                    initIO();
                }

                // Try to read the available data from the serial input stream, trim it and try to
                // parse response from it.
                if (this.input != null) {
                    int bytesAvailable = readSerialData(buffer);
                    if (bytesAvailable > 0) {
                        trimSerialData(buffer);
                        checkForResponse(buffer);
                    }
                }

                SystemClock.sleep(20);
            }
        } finally {
            running = false;
            Log.d(this.getClass().getSimpleName(), "Worker stopped");
        }
    }

    /**
     * Send the specified response to the listener if there is a command currently executing.
     *
     * The respons is send in non-blocking manner in another thread.
     * @param response the response
     * @param isComplete true iff the response is complete. False if we've hit timeout and there
     *                   is some available data that is being send as response.
     */
    private void respond(final String response, final boolean isComplete) {
        if (this.currentCommand == null) {
            // No command was send, no need for posting a replay.
            return;
        }
        this.currentCommand = null;

        Executor.execute(new Runnable() {
            @Override
            public void run() {
                if (SerialWorker.this.responseListener != null) {
                    String re = response.replace("\n", "").replace("\r", "");
                    try {
                        if (isComplete) {
                            responseListener.onResponse(re);
                        } else {
                            responseListener.onIncompleteResponse(re);
                        }
                    } catch (RuntimeException ex) {
                        Log.d(this.getClass().getSimpleName(),
                                "Got exception for " + re, ex);
                    }
                }
            }
        });
    }

    /**
     * Send an error response to the current listener if there is a command currently executing.
     *
     * The error is send in non-blocking manner in another thread.
     * @param error the error to be send.
     */
    private void respond(final Exception error) {
        if (this.currentCommand == null) {
            // No command was send, no need for posting an error.
            return;
        }
        this.currentCommand = null;

        Executor.execute(new Runnable() {
            @Override
            public void run() {
                if (SerialWorker.this.responseListener != null) {
                    Log.d(this.getClass().getSimpleName(), "Responding with error", error);
                    try {
                        responseListener.onError(error);
                    } catch (RuntimeException ex) {
                        Log.d(this.getClass().getSimpleName(),
                                "Got exception on .onError()", ex);
                    }
                }
            }
        });
    }

    private void initIO() {
        cleanUp();

        Log.d(this.getClass().getSimpleName(), "Initializing bluetooth serial connection.");

        try {
            this.device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(this.deviceAddress);
            //Standard SerialPortService ID
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            SystemClock.sleep(1);
            try {
                this.socket = this.device.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException ex) {
                Method createMethod = device.getClass()
                        .getMethod("createInsecureRfcommSocket", new Class[] { int.class });
                this.socket = (BluetoothSocket)createMethod.invoke(device, 1);
            }
            this.socket.connect();
            this.output = this.socket.getOutputStream();
            this.input = this.socket.getInputStream();
            Log.d(this.getClass().getSimpleName(), "Initialization completed, connection established.");
        } catch (Exception ex) {
            Log.e(
                    this.getClass().getSimpleName(),
                    "Failed to establish bluetooth serial connection.",
                    ex);
        }
    }

    private int readSerialData(StringBuilder buffer) {
        try {
            int bytesAvailable = this.input.available();
            if (bytesAvailable > 0) {
                byte[] packetBytes = new byte[bytesAvailable];
                this.input.read(packetBytes);
                buffer.append(new String(packetBytes));
            }
            return bytesAvailable;
        } catch (IOException ex) {
            Log.d(
                this.getClass().getSimpleName(),
                "Got exception while reading the bluetooth serial input stream.",
                ex);
            cleanUp();
        }
        return 0;
    }

    private void trimSerialData(StringBuilder buffer) {
        String trimmed = buffer.toString().replace("\n", "").replace("\r", "");
        if (this.currentCommand != null && trimmed.startsWith(this.currentCommand)) {
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
        buffer.setLength(0);
        buffer.append(trimmed);
    }

    private void checkForResponse(StringBuilder data) {
        String current = data.toString();
        if (current.contains(">")) {
            // Extract all data before the first '>' char and use it as response.
            String response = current.substring(0, current.indexOf('>'));

            // Empty the buffer. SerialWorker is working with only one command at time, so any
            // excessive data here can be purged without side effects.
            data.setLength(0);

            respond(response, true);
        }
    }

    public boolean isRunning() {
        return this.running;
    }
}