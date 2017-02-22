package com.brum.wgdiag.command.impl;

import com.brum.wgdiag.command.diag.Field;

import java.math.BigDecimal;
import java.text.Format;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Common diagnostic command utils.
 */

public class DiagUtils {
    public static class DiagCommand extends com.brum.wgdiag.command.impl.Utils.BasicCommand implements com.brum.wgdiag.command.diag.DiagCommand {
        private final List<Field> fields;

        public DiagCommand(String request,
                           String responseHeader,
                           long timeout,
                           List<Field> fields) {
            super(request, responseHeader, timeout);
            this.fields = Collections.unmodifiableList(fields);
        }

        @Override
        public Map<String, String> parseResponse(String response) {
            Map<String, String> result = new HashMap<>();
            for (Field field : this.fields) {
                result.put(field.getKey(), field.toString(response));
            }
            return result;
        }

        @Override
        public Map<String, BigDecimal> parseResponseValues(String response) {
            Map<String, BigDecimal> result = new HashMap<>();
            for (Field field : this.fields) {
                result.put(field.getKey(), field.toDecimal(response));
            }
            return result;
        }

        @Override
        public List<Field> getDiagFields() {
            return this.fields;
        }
    }

    public static com.brum.wgdiag.command.diag.DiagCommand createCommand(final String request,
                                                                          final String expectedResponseHeader,
                                                                          final long timeout,
                                                                          final List<Field> fields) {
        return new DiagCommand(request, expectedResponseHeader, timeout, fields);
    }

    public static Field createTextField(final String key, final String description) {
        return new Field() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public String toString(String response) {
                return response;
            }

            @Override
            public BigDecimal toDecimal(String response) {
                return new BigDecimal(response.replaceAll("[^\\d.]", ""));
            }
        };
    }

    public static Field createField(final int startPos,
                                    final int length,
                                    final BigDecimal offset,
                                    final BigDecimal factor,
                                    final Format format,
                                    final String key,
                                    final String description) {



        return new Field() {
            @Override
            public String getKey() { return key; }

            @Override
            public String getDescription() { return description; }

            @Override
            public BigDecimal toDecimal(String response) {
                String[] tokens = response.split(" ");

                StringBuilder hex = new StringBuilder();

                for (int i = startPos; i < startPos + length; i++) {
                    hex.append(tokens[i]);
                }

                Long rawValue = Long.parseLong(hex.toString(), 16);
                if (rawValue > 32767) {
                    rawValue -= 65536;
                }
                BigDecimal value = new BigDecimal(rawValue);
                value = value.divide(factor);
                value = value.add(offset);
                return value;
            }

            @Override
            public String toString(String response) {
                return format.format(toDecimal(response));
            }
        };
    }

    public static Field createField(final int startPos,
                                    Format format,
                                    final String key,
                                    final String description) {
        return createField(startPos, 2, new BigDecimal(0), new BigDecimal(1), format, key, description);
    }

    public static Iterator<com.brum.wgdiag.command.diag.DiagCommand> createEndlessIterator(final List<com.brum.wgdiag.command.diag.DiagCommand> initCommands,
                                                                                            final List<com.brum.wgdiag.command.diag.DiagCommand> diagCommands) {
        return new Iterator<com.brum.wgdiag.command.diag.DiagCommand>() {
            private int position = 0;
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public com.brum.wgdiag.command.diag.DiagCommand next() {
                com.brum.wgdiag.command.diag.DiagCommand next = null;
                if (position < initCommands.size()) {
                    next = initCommands.get(position);
                } else {
                    int next_pos = position - initCommands.size();
                    next_pos = next_pos % diagCommands.size();
                    next = diagCommands.get(next_pos);
                }
                position++;
                return next;
            }

        };
    }

//    public static void main(String[] args) {
//        Iterator<DiagCommand> iter = createEndlessIterator(InitCommands.VERIFY_DEVICE_COMMANDS, InitCommands.VERIFY_DEVICE_COMMANDS);
//        int i = 0;
//        System.out.println(iter);
//        while (i < 22 && iter.hasNext()) {
//            DiagCommand n = iter.next();
//            System.out.println(n.getRequestCommand());
//            i++;
//        }
//    }


}
