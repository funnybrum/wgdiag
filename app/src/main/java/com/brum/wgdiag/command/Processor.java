package com.brum.wgdiag.command;

import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.brum.wgdiag.activity.utils.ExecutionInterrupter;
import com.brum.wgdiag.bluetooth.Constants;
import com.brum.wgdiag.bluetooth.ResponseListener;
import com.brum.wgdiag.bluetooth.ResponseListenerEx;
import com.brum.wgdiag.bluetooth.Service;
import com.brum.wgdiag.command.diag.DataHandler;
import com.brum.wgdiag.command.diag.DiagCommand;
import com.brum.wgdiag.command.diag.Field;
import com.brum.wgdiag.command.diag.Package;
import com.brum.wgdiag.util.Executor;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Command processor. Takes care of initializing the bluetooth communication and executing
 * command diagnostic packages.
 */

public class Processor {

    /**
     * Block and execute the init commands. If device is not connected or the device verification
     * fails - stop the SerialWorker (it is no longer needed in such cases).
     * @return true iff all commands are executed and responses are as expected.
     */
    public static boolean verifyDevice(String address) {
        Log.d(Processor.class.getSimpleName(), "Verifying device...");
        Service.init(address);

        List<Command> initCommands = Constants.VERIFY_DEVICE_COMMANDS;

        for (final Command cmd : initCommands) {
            if (execAndVerify(cmd, null) != ExecResult.SUCCESS) {
                Service.stop();
                return false;
            }
        }

        return true;
    }

    /**
     * Continuous diag package executor. Execute first the init commands and fail if any of them
     * fails. If all succeed - iterate over the diag commands and on each successful response -
     * update the UI using the diag data handler.
     * @param pkg The diagnostic commands package.
     * @param handler Handler for updating the UI.
     */
    public static ExecutionInterrupter executeDiagPackage(final Package pkg,
                                                          final DataHandler handler,
                                                          final Handler errorHandler,
                                                          final Activity activity) {
        final AtomicBoolean interrupt = new AtomicBoolean(false);

        final Runnable processorRunnable = new Runnable() {
            @Override
            public void run() {
                final AtomicBoolean initiliazed = new AtomicBoolean(false);
                final AtomicBoolean executed = new AtomicBoolean(false);
                Iterator<DiagCommand> iterator = pkg.getCommandIterator();

                while (!interrupt.get()) {
                    if (initiliazed.get() == false) {
                        for (final Command cmd : pkg.getInitCommands()) {
                            ExecResult result = execAndVerify(cmd, interrupt);

                            if (result == ExecResult.FAILURE) {
                                errorHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(
                                                activity,
                                                "Failed on " + cmd.getRequestCommand() + ", aborting.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                                activity.finish();
                                return;
                            } else if (result == ExecResult.INTERRUPTED) {
                                break;
                            }
                        }
                        initiliazed.set(true);
                    }

                    if (interrupt.get()) {
                        break;
                    }

                    final DiagCommand cmd = iterator.next();
                    executed.set(false);

                    Service.setResponseListener(new ResponseListenerEx() {
                        @Override
                        public void onResponse(String response) {
                            executed.set(true);
                            if (interrupt.get()) {
                                return;
                            }
                            if (cmd.verifyResponse(response)) {
                                Map<String, String> stringData = cmd.parseResponse(response);
                                Map<String, BigDecimal> decimalData = cmd.parseResponseValues(response);
                                for (String key : stringData.keySet()) {
                                    handler.handle(key, stringData.get(key));
                                    handler.handle(key, decimalData.get(key));
                                }
                            }
                        }

                        @Override
                        public void onIncompleteResponse(String response) {
                            executed.set(true);
                            if (interrupt.get()) {
                                return;
                            }
                            for (Field field : cmd.getDiagFields()) {
                                handler.handle(field.getKey(), "NA");
                                handler.handle(field.getKey(), BigDecimal.ZERO);
                            }
                        }

                        @Override
                        public void onError(Exception ex) {
                            initiliazed.set(false);
                        }
                    });

                    Service.write(cmd);
                    while (executed.get() == false && interrupt.get() == false) {
                        SystemClock.sleep(10);
                    }
                }
            }

            public void stop() {
                interrupt.set(true);
            }
        };

        Executor.execute(processorRunnable);

        return new ExecutionInterrupter() {
            @Override
            public void interrupt(boolean block) {
                interrupt.set(true);
                while (block && Executor.isRunning(processorRunnable)) {
                    SystemClock.sleep(10);
                }
            }
        };
    }

    private static enum ExecResult {
        SUCCESS, FAILURE, INTERRUPTED
    }

    private static ExecResult execAndVerify(final Command cmd, AtomicBoolean interrupt) {
        final AtomicBoolean executed = new AtomicBoolean(false);
        final AtomicBoolean success = new AtomicBoolean(false);

        Service.setResponseListener(new ResponseListener() {
            @Override
            public void onResponse(String response) {
                Log.d("Processor", "onResponse()");
                success.set(cmd.verifyResponse(response));
                executed.set(true);
            }

            @Override
            public void onIncompleteResponse(String response) {
                Log.d("Processor", "onImcompleteResponse()");

                success.set(false);
                executed.set(true);
            }
        });

        if (interrupt != null && interrupt.get()) {
            return ExecResult.INTERRUPTED;
        }

        Service.write(cmd);

        while (executed.get() == false) {
            if (interrupt != null && interrupt.get()) {
                return ExecResult.INTERRUPTED;
            }
            SystemClock.sleep(10);
        }

        return (success.get()) ? ExecResult.SUCCESS : ExecResult.FAILURE;
    }
}
