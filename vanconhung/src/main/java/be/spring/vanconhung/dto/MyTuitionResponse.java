package be.spring.vanconhung.dto;

import java.time.Instant;
import java.util.List;

import be.spring.vanconhung.entity.TuitionStatus;

public record MyTuitionResponse(
        int year,
        int month,
        List<ClassTuition> classes) {

    public record ClassTuition(
            Long classId,
            String className,
            long feePerSession,
            int sessionCount,
            long amount,
            TuitionStatus status,
            Instant requestedAt,
            Instant paidAt,
            String note) {
    }
}
