package com.brum.wgdiag.activity.utils;

import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Collections;
import java.util.Map;

/**
 * Handler for responses send by diagnostic commands.
 */
public class DiagDataHandler extends Handler {

    private final Map<String, TextView> dataViewers;

    private DiagDataHandler(Map<String, TextView> dataViewers) {
        this.dataViewers = Collections.unmodifiableMap(dataViewers);
    }

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
    public void handleMessage(Message msg) {
        String key = msg.getData().get("key").toString();
        String value = msg.getData().get("value").toString();

        TextView view = dataViewers.get(key);
        view.setText(value);
        view.postInvalidate();
    }

    public static class Factory {
        public static DiagDataHandler create(Map<String, TextView> dataViewers) {
            return new DiagDataHandler(dataViewers);
        }
    }
}
