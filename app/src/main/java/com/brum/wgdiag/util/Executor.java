package com.brum.wgdiag.util;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
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
    private static WeakHashMap<Activity, String> references = new WeakHashMap<>();

    private static Runnable cleaner = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (Executor.references.isEmpty()) {
                    Log.d(Executor.class.getSimpleName(), "Stopping...");
                    for (Runnable runnable : running) {
                        try {
                            Method stopMethod = running.getClass().getMethod("stop");
                            stopMethod.invoke(runnable);
                        } catch (Exception ex) {
                            // Ignore exceptions. They will be thrown only for runnables that don't have stop
                            // method.
                        }
                    }
                    ThreadPoolExecutor e = Executor.executor;
                    Executor.executor = null;
                    SystemClock.sleep(100);
                    Log.d(Executor.class.getSimpleName(), "Stopped?");
                    e.shutdownNow();
                    // Unlikely to get to that point, the thread should already be killed. But let's
                    // stay on the safe side :) .
                    break;
                }
                SystemClock.sleep(1000);
            }
        }
    };

    private static void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }

        Log.d(Executor.class.getSimpleName(), "Starting");

        executor = new ThreadPoolExecutor(
                5, 5,
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

    public static void bind(Activity activity) {
        start();
        Executor.references.put(activity, activity.getClass().getSimpleName());
        if (!Executor.isRunning(Executor.cleaner)) {
            Executor.execute(cleaner);
        }
    }

    public static void unbind(Activity activity) {
        Executor.references.remove(activity);
    }

    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    public static boolean isRunning(Runnable runnable) {
        return running.contains(runnable);
    }
}
