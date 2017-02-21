package com.brum.wgdiag.command.diag.impl;

import com.brum.wgdiag.command.Command;
import com.brum.wgdiag.command.diag.DiagCommand;
import com.brum.wgdiag.command.diag.Field;
import com.brum.wgdiag.command.diag.Package;
import com.brum.wgdiag.command.impl.DiagUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Diagnostic command package implementation.
 */

public class PackageImpl implements Package {
    private final List<DiagCommand> commands;
    private final List<Command> initCommands;
    private final String description;
    private final String name;

    public PackageImpl(String name,
                       String description,
                       List<Command> initCommands,
                       List<DiagCommand> commands) {
        this.initCommands = initCommands;
        this.commands = commands;
        this.description = description;
        this.name = name;
    }

    @Override
    public Iterator<DiagCommand> getCommandIterator() {
        return DiagUtils.createEndlessIterator(Collections.<DiagCommand>emptyList(), this.commands);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<Field> getFields() {
        Set<String> fieldKeys = new HashSet<>();
        List<Field> fields = new ArrayList<>();

        for (DiagCommand command : this.commands) {
            for (Field field : command.getDiagFields()) {
                if (!fieldKeys.contains(field.getKey())) {
                    fieldKeys.add(field.getKey());
                    fields.add(field);
                }
            }
        }

        return fields;
    }

    @Override
    public List<Command> getInitCommands() {
        return this.initCommands;
    }
}
