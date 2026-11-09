
package com.report.worker.service;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ProcessingTokenRegistry {

    private final ConcurrentMap<String, Lease> leases =
            new ConcurrentHashMap<>();

    public void store(
            String messageId,
            String trackingId,
            String processingToken
    ) {
        leases.put(messageId, new Lease(trackingId, processingToken));
    }

    public String get(String messageId) {
        Lease lease = leases.get(messageId);
        return lease == null ? null : lease.processingToken();
    }

    public void remove(String messageId) {
        leases.remove(messageId);
    }

    public Collection<Map.Entry<String, Lease>> getActiveLeases() {
        return List.copyOf(leases.entrySet());
    }

    public record Lease(String trackingId, String processingToken) {
    }
}