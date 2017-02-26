package com.brum.wgdiag.logger;

import android.os.SystemClock;

import com.brum.wgdiag.command.Command;
import com.brum.wgdiag.command.diag.DiagCommand;
import com.brum.wgdiag.command.diag.Field;
import com.brum.wgdiag.command.diag.Package;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
 * Dynamically adapting data logger. Creates CSV containing timestamp column and a column for each
 * logged property.
 */
public class DiagDataLogger {
    private static LinkedHashSet<String> currentCommandFields = new LinkedHashSet<>();
    private static Map<String, String> currentRow = new HashMap<>();
    private static ByteArrayOutputStream file = new ByteArrayOutputStream();
    private static final long startTime = SystemClock.elapsedRealtime();

    public static void setDiagPackage(Package pkg) {
        boolean emptyFile = false;
        if (currentCommandFields.isEmpty()) {
            emptyFile = true;
        }
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

        if (!emptyFile) {
            // Put an emtpy line to separate the different data sets.
            DiagDataLogger.writeRow(null, Collections.<String>emptyList());
            DiagDataLogger.writeRow(null, Collections.<String>emptyList());
        }
        writeRow("timestamp", DiagDataLogger.currentCommandFields);
        DiagDataLogger.currentRow = new HashMap<>();
    }

    public static void addData(String field, BigDecimal value) {
        DiagDataLogger.currentRow.put(field, value.toString());

        if (DiagDataLogger.currentRow.keySet().equals(DiagDataLogger.currentCommandFields)) {
            List<String> values = new ArrayList<>(DiagDataLogger.currentCommandFields.size());
            for (String key : DiagDataLogger.currentCommandFields) {
                values.add(DiagDataLogger.currentRow.get(key));
            }
            Long timestamp = (SystemClock.elapsedRealtime() - DiagDataLogger.startTime)/100;
            String ts = new BigDecimal(timestamp).divide(new BigDecimal(10)).toString();
            writeRow(ts, values);
        }
    }

    /**
     * Get the current file content.
     *
     * @return the file represented as ASCII encoded byte array.
     */
    public static File getLogFile(File destinationFolder) throws IOException {
        File logFile = File.createTempFile("log", ".csv", destinationFolder);
        FileOutputStream logFileOs = new FileOutputStream(logFile);
        logFileOs.write(DiagDataLogger.file.toByteArray());
        logFileOs.flush();
        logFileOs.close();
        logFile.setReadable(true, false);

        return logFile;
    }

    /**
     * Clear the current log file and release all resource;
     */
    public static void reset() {
        DiagDataLogger.file = new ByteArrayOutputStream();
    }

    private static void writeRow(String timestampColumnValue, Collection<String> items) {
        try {
            if (timestampColumnValue != null) {
                DiagDataLogger.file.write(timestampColumnValue.getBytes());
                DiagDataLogger.file.write(',');
            }
            for (String item : items) {
                DiagDataLogger.file.write(item.getBytes());
                DiagDataLogger.file.write(',');
            }
            DiagDataLogger.file.write('\n');
        } catch (IOException ex) {
            android.util.Log.d("DDL", "Got ignored exception", ex);
        }
    }
}
