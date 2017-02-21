package com.brum.wgdiag.command;

import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.Toast;

import com.brum.wgdiag.activity.utils.DiagDataHandler;
import com.brum.wgdiag.activity.utils.ExecutionInterrupter;
import com.brum.wgdiag.bluetooth.Constants;
import com.brum.wgdiag.bluetooth.ResponseListener;
import com.brum.wgdiag.bluetooth.Service;
import com.brum.wgdiag.command.diag.DiagCommand;
import com.brum.wgdiag.command.diag.Field;
import com.brum.wgdiag.command.diag.Package;

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
     * Block and execute the init commands.
     * @return true iff all commands are executed and responses are as expected.
     */
    public static boolean verifyDevice(String address) {
        Service.stop();
        Service.start(address);

        List<Command> initCommands = Constants.VERIFY_DEVICE_COMMANDS;

        for (final Command cmd : initCommands) {
            if (execAndVerify(cmd, null) != ExecResult.SUCCESS) {
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
                                                          final DiagDataHandler handler,
                                                          final Handler errorHandler,
                                                          final Activity activity) {
        final AtomicBoolean interrupt = new AtomicBoolean(false);

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
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

                Iterator<DiagCommand> iterator = pkg.getCommandIterator();
                final AtomicBoolean executed = new AtomicBoolean(false);

                while (!interrupt.get()) {
                    final DiagCommand cmd = iterator.next();
                    executed.set(false);

                    Service.setResponseListener(new ResponseListener() {
                        @Override
                        public void onResponse(String response) {
                            executed.set(true);
                            if (interrupt.get()) {
                                return;
                            }
                            if (cmd.verifyResponse(response)) {
                                Map<String, String> data = cmd.parseResponse(response);
                                for (String key : data.keySet()) {
                                    handler.handle(key, data.get(key));
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
                            }
                        }
                    });

                    Service.write(cmd);
                    while (executed.get() == false && interrupt.get() == false) {
                        SystemClock.sleep(10);
                    }
                }
            }
        });

        thread.start();

        return new ExecutionInterrupter() {
            @Override
            public void interrupt(boolean block) {
                interrupt.set(true);
                while (block && thread.isAlive()) {
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
                success.set(cmd.verifyResponse(response));
                executed.set(true);
            }

            @Override
            public void onIncompleteResponse(String response) {
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
