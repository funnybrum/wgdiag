package com.brum.wgdiag.util;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Static executor that will run the required threads. Such (but not only) as the serial IO worker
 * and the command processor.
 *
 * All running runnables that have defined method named stop() without argumetns will have this
 * method invoked before they are forcefully killed. This will happen when the executor is stopping.
 */
public class Executor {

    private static ThreadPoolExecutor executor = null;
    private static Set<Runnable> running = new HashSet<>();

    public static void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }

        executor = new ThreadPoolExecutor(
                4, 4,
                5, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                running.add(r);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                running.remove(r);
            }
        };
    }

    public static void stop() {
        if (executor == null) {
            return;
        }

        for (Runnable runnable : running) {
            try {
                Method stopMethod = running.getClass().getMethod("stop");
                stopMethod.invoke(runnable);
            } catch (Exception ex) {
                // Ignore exceptions. They will be thrown only for runnables that don't have stop
                // method.
            }
        }

        executor.shutdownNow();
    }

    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    public static boolean isRunning(Runnable runnable) {
        return running.contains(runnable);
    }
}
