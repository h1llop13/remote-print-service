package com.remoteprint.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrintJobRepository extends JpaRepository<PrintJob, UUID> {

    List<PrintJob> findByStatusOrderByCreatedAtAsc(
            PrintJobStatus status
    );

    List<PrintJob> findByStatusInOrderByCreatedAtAsc(
            List<PrintJobStatus> status
    );
}
