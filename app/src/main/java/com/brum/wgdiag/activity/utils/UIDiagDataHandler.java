package com.brum.wgdiag.activity.utils;

import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.brum.wgdiag.command.diag.DataHandler;
import com.brum.wgdiag.command.diag.Package;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * Handler for responses send by diagnostic commands.
 */
public class UIDiagDataHandler extends Handler implements DataHandler {

    private final Map<String, TextView> dataViewers;

    public UIDiagDataHandler(Map<String, TextView> dataViewers) {
        this.dataViewers = Collections.unmodifiableMap(dataViewers);
    }

    @Override
    public void handle(String key, String value) {
        if (!dataViewers.keySet().contains(key)) {
            return;
        }

        Message msg = new Message();
        msg.getData().putString("key", key);
        msg.getData().putString("value", value);
        sendMessage(msg);
    }

    @Override
    public void handle(String key, BigDecimal value) {
        // Do nothing.
    }

    @Override
    public void handleMessage(Message msg) {
        String key = msg.getData().get("key").toString();
        String value = msg.getData().get("value").toString();

        TextView view = dataViewers.get(key);
        view.setText(value);
        view.postInvalidate();
    }

    @Override
    public void switchPackage(Package pkg) {
        // Do nothing. The UIDataHandler is designed for single package and is initialized
        // according it.
    }
}
