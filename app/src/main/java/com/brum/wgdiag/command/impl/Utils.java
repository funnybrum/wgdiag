package com.brum.wgdiag.command.impl;

import com.brum.wgdiag.command.Command;

/**
 * Common command utils.
 */
public class Utils {

    public static class BasicCommand implements Command {
        private final String request;
        private final String responseHeader;
        private final Long timeout;

        public BasicCommand(final String request,
                            final String responseHeader,
                            final long timeout) {
            this.request = request;
            this.responseHeader = responseHeader;
            this.timeout = timeout;
        }

        @Override
        public String getRequestCommand() {
            return request;
        }

        @Override
        public boolean verifyResponse(String response) {
            return response != null && (
                    responseHeader == null ||
                            response.startsWith(responseHeader)
            );
        }

        @Override
        public long getTimeoutMillis() {
            return timeout;
        }
    }

    public static Command createCommand(String request,
                                        String responseHeader,
                                        long timeout) {
        return new BasicCommand(request, responseHeader, timeout);
    }

    public static Command createCommand(String request, String responseHeader) {
        return createCommand(request, responseHeader, 2000L);
    }
}