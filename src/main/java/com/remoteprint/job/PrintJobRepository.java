package com.remoteprint.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrintJobRepository extends JpaRepository<PrintJob, UUID> {
}
