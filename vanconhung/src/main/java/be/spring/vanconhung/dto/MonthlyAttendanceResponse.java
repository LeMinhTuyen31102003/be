package be.spring.vanconhung.dto;

import java.util.List;
import java.util.Map;

import be.spring.vanconhung.entity.AttendanceStatus;

public record MonthlyAttendanceResponse(
        Long classId,
        String className,
        int year,
        int month,
        List<String> sessionDates,
        List<StudentAttendanceRow> students) {

    public record StudentAttendanceRow(
            Long studentId,
            String fullName,
            Map<String, AttendanceEntry> entries,
            AttendanceSummary summary) {
    }

    public record AttendanceEntry(AttendanceStatus status, String note) {
    }

    public record AttendanceSummary(long present, long absent, long late, long excused) {
    }
}
