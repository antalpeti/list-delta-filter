package com.antalpeti.listsetdifference.exception;

/**
 * Thrown when a requested upload ID cannot be found (or does not belong to the
 * specified section).  Controllers should map this to HTTP 404.
 */
public class UploadNotFoundException extends RuntimeException {

    public UploadNotFoundException(String uploadId) {
        super("Upload not found: " + uploadId);
    }
}

