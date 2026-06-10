package com.wh.jobsbackend.worker;

public class PlatformPageModelException extends IllegalStateException {
    public PlatformPageModelException(String message) {
        super(message);
    }

    public PlatformPageModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
