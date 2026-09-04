package be.spring.vanconhung.dto;

import java.util.List;

import be.spring.vanconhung.entity.AttendanceStatus;

public record MyAttendanceResponse(
        int year,
        int month,
        List<ClassAttendance> classes) {

    public record ClassAttendance(
            Long classId,
            String className,
            List<SessionEntry> sessions,
            AttendanceSummary summary) {
    }

    public record SessionEntry(String date, AttendanceStatus status) {
    }

    public record AttendanceSummary(long present, long absent, long late, long excused) {
    }
}
