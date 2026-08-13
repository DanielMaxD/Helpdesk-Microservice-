package com.helpdesk.ticketservice.exception;

/**
 * Thrown when user-service cannot be reached or fails unexpectedly during a
 * server-to-server verification call (e.g. agent-assignment checks). Keeps the
 * internal cause out of the response - the client only sees a generic 503.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
