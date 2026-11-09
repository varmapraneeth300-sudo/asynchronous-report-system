package com.report.api.repo;

import com.report.api.entity.ReportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReportJobRepo extends JpaRepository<ReportJob, Long> {

    Optional<ReportJob> findByTrackingId(String trackingId);
}