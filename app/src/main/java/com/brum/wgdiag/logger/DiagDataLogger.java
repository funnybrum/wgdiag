package com.brum.wgdiag.logger;

import com.brum.wgdiag.command.Command;
import com.brum.wgdiag.command.diag.DiagCommand;
import com.brum.wgdiag.command.diag.Field;
import com.brum.wgdiag.command.diag.Package;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by brum on 2/21/17.
 */

public class DiagDataLogger {
    private static LinkedHashSet<String> currentCommandFields = new LinkedHashSet<>();
    private static Map<String, String> currentRow = new HashMap<>();
    private static ByteArrayOutputStream file = new ByteArrayOutputStream();

    public static void setDiagPackage(Package pkg) {
        DiagDataLogger.currentCommandFields = new LinkedHashSet<>();

        Set<String> processedCommands = new HashSet<>();
        Iterator<DiagCommand> cmdIter = pkg.getCommandIterator();
        while (true) {
            DiagCommand cmd = cmdIter.next();
            if (processedCommands.contains(cmd.getRequestCommand())) {
                break;
            }
            processedCommands.add(cmd.getRequestCommand());

            for (Field f : cmd.getDiagFields()) {
                currentCommandFields.add(f.getKey());
            }
        }

        writeRow(DiagDataLogger.currentCommandFields);
        DiagDataLogger.currentRow = new HashMap<>();
    }

    public static void addData(String field, BigDecimal value) {
        DiagDataLogger.currentRow.put(field, value.toString());

        if (DiagDataLogger.currentRow.keySet().equals(DiagDataLogger.currentCommandFields)) {
            List<String> values = new ArrayList<>(DiagDataLogger.currentCommandFields.size());
            for (String key : DiagDataLogger.currentCommandFields) {
                values.add(DiagDataLogger.currentRow.get(key));
            }
            writeRow(values);
        }
    }

    /**
     * Get the current file content.
     *
     * @return the file represented as ASCII encoded byte array.
     */
    public static byte[] getFile() {
        return null;
    }

    /**
     * Clear the current log file and release all resource;
     */
    public static void reset() {
        DiagDataLogger.file = new ByteArrayOutputStream();
    }

    private static void writeRow(Collection<String> items) {
        try {
            for (String item : items) {
                DiagDataLogger.file.write(item.getBytes());
                DiagDataLogger.file.write(',');
            }
            DiagDataLogger.file.write('\n');
            DiagDataLogger.file.write('\r');
        } catch (IOException ex) {
            // Ignore.
        }
    }
}
