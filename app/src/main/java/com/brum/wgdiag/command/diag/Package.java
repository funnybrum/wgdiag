package com.brum.wgdiag.command.diag;

import com.brum.wgdiag.command.Command;

import java.util.Iterator;
import java.util.List;

/**
 * Diagnostic command package. Provides command iterator that will return all commands that should
 * be executed, short description and list of all fields that can be retrieved by this package.
 */
public interface Package {

    /**
     * Get the commands iterator.
     */
    List<Command> getInitCommands();

    /**
     * Get the commands iterator.
     */
    Iterator<DiagCommand> getCommandIterator();

    /**
     * Package name.
     */
    String getName();

    /**
     * Description of the command package.
     */
    String getDescription();

    /**
     * Fields that can be retrieved by this package.
     */
    List<Field> getFields();

}
