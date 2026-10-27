package com.rian.appointment_api.exception;

public class ProtocolUnavailableException extends RuntimeException {

    public ProtocolUnavailableException(String message) {
        super(message);
    }

    public ProtocolUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
