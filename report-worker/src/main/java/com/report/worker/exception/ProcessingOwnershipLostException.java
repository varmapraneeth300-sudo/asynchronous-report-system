package com.report.worker.exception;

public class ProcessingOwnershipLostException extends RuntimeException {

    public ProcessingOwnershipLostException(String trackingId) {
        super("Processing ownership lost for tracking ID: " + trackingId);
    }
}