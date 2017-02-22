package com.brum.wgdiag.command.diag.impl;

import com.brum.wgdiag.command.diag.DataHandler;
import com.brum.wgdiag.command.diag.Package;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;

/**
 * Orchestrate a list of data handlers.
 */
public class CompositeDataHandler implements DataHandler {
    private HashMap<String, DataHandler> handlers = new HashMap<>();

    public void registerHandler(String handlerId, DataHandler handler) {
        this.handlers.put(handlerId, handler);
    }

    public void unregisterHandler(String handlerId) {
        handlers.remove(handlerId);
    }

    @Override
    public void handle(String key, String value) {
        Collection<DataHandler> handlers = this.handlers.values();
        for (DataHandler handler : handlers) {
            handler.handle(key, value);
        }
    }

    @Override
    public void handle(String key, BigDecimal value) {
        Collection<DataHandler> handlers = this.handlers.values();
        for (DataHandler handler : handlers) {
            handler.handle(key, value);
        }
    }

    @Override
    public void switchPackage(Package pkg) {
        Collection<DataHandler> handlers = this.handlers.values();
        for (DataHandler handler : handlers) {
            handler.switchPackage(pkg);
        }
    }
}
