package be.spring.vanconhung.dto;

import java.time.Instant;
import java.util.List;

import be.spring.vanconhung.entity.TuitionStatus;

public record MonthlyTuitionResponse(
        Long classId,
        String className,
        Long feePerSession,
        int year,
        int month,
        List<StudentTuitionRow> students,
        TuitionSummary summary) {

    public record StudentTuitionRow(
            Long studentId,
            String fullName,
            int sessionCount,
            long amount,
            TuitionStatus status,
            Instant requestedAt,
            Instant paidAt,
            String note) {
    }

    public record TuitionSummary(
            int totalStudents,
            int paidCount,
            int pendingCount,
            long totalDue,
            long totalCollected,
            long totalOutstanding) {
    }
}
