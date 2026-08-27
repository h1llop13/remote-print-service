package com.remoteprint.job;

import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPrintJobRepository {

    private final Map<UUID, PrintJob> jobs =
            new ConcurrentHashMap<>();

    public PrintJob save(PrintJob job) {
        jobs.put(job.getId(), job);
        return job;
    }

    public Optional<PrintJob> findById(UUID id) {
        return Optional.ofNullable(jobs.get(id));
    }

    public List<PrintJob> findAll() {
        return new ArrayList<>(jobs.values());
    }
}
