package be.spring.vanconhung.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.spring.vanconhung.dto.MarkAttendanceRequest;
import be.spring.vanconhung.dto.MonthlyAttendanceResponse;
import be.spring.vanconhung.entity.Attendance;
import be.spring.vanconhung.entity.AttendanceStatus;
import be.spring.vanconhung.entity.ClassRoom;
import be.spring.vanconhung.entity.ClassSchedule;
import be.spring.vanconhung.entity.User;
import be.spring.vanconhung.repository.AttendanceRepository;
import be.spring.vanconhung.repository.ClassRoomRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classes/{classId}/attendance")
@Transactional
public class AttendanceController {

    private final ClassRoomRepository classRoomRepository;
    private final AttendanceRepository attendanceRepository;

    public AttendanceController(ClassRoomRepository classRoomRepository, AttendanceRepository attendanceRepository) {
        this.classRoomRepository = classRoomRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping
    public ResponseEntity<MonthlyAttendanceResponse> monthly(@PathVariable Long classId,
            @RequestParam int year, @RequestParam int month) {
        return classRoomRepository.findById(classId)
                .map(classRoom -> ResponseEntity.ok(buildMonthlyResponse(classRoom, year, month)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{date}")
    public ResponseEntity<MonthlyAttendanceResponse> mark(@PathVariable Long classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody MarkAttendanceRequest request) {
        Optional<ClassRoom> classRoomOpt = classRoomRepository.findById(classId);
        if (classRoomOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ClassRoom classRoom = classRoomOpt.get();
        Map<Long, User> studentsById = classRoom.getStudents().stream()
                .collect(Collectors.toMap(User::getId, s -> s));

        for (MarkAttendanceRequest.StudentAttendanceInput input : request.records()) {
            User student = studentsById.get(input.studentId());
            if (student == null) {
                continue;
            }

            Optional<Attendance> existing = attendanceRepository
                    .findByClassRoom_IdAndStudent_IdAndDate(classId, input.studentId(), date);

            if (input.status() == null) {
                existing.ifPresent(attendanceRepository::delete);
                continue;
            }

            Attendance attendance = existing.orElseGet(Attendance::new);
            if (attendance.getId() == null) {
                attendance.setClassRoom(classRoom);
                attendance.setStudent(student);
                attendance.setDate(date);
            }
            attendance.setStatus(input.status());
            attendance.setNote(input.note());
            attendanceRepository.save(attendance);
        }

        return ResponseEntity.ok(buildMonthlyResponse(classRoom, date.getYear(), date.getMonthValue()));
    }

    private MonthlyAttendanceResponse buildMonthlyResponse(ClassRoom classRoom, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<Attendance> attendances = attendanceRepository
                .findByClassRoom_IdAndDateBetween(classRoom.getId(), start, end);

        Set<DayOfWeek> scheduledDays = classRoom.getSchedules().stream()
                .map(ClassSchedule::getDayOfWeek)
                .collect(Collectors.toSet());

        TreeSet<LocalDate> sessionDates = new TreeSet<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (scheduledDays.contains(d.getDayOfWeek())) {
                sessionDates.add(d);
            }
        }
        attendances.forEach(a -> sessionDates.add(a.getDate()));

        Map<Long, Map<String, MonthlyAttendanceResponse.AttendanceEntry>> byStudent = new HashMap<>();
        for (Attendance a : attendances) {
            byStudent
                    .computeIfAbsent(a.getStudent().getId(), k -> new HashMap<>())
                    .put(a.getDate().toString(),
                            new MonthlyAttendanceResponse.AttendanceEntry(a.getStatus(), a.getNote()));
        }

        List<MonthlyAttendanceResponse.StudentAttendanceRow> rows = classRoom.getStudents().stream()
                .sorted(Comparator.comparing(User::getFullName))
                .map(student -> {
                    Map<String, MonthlyAttendanceResponse.AttendanceEntry> entries = byStudent
                            .getOrDefault(student.getId(), Map.of());
                    long present = countStatus(entries, AttendanceStatus.PRESENT);
                    long absent = countStatus(entries, AttendanceStatus.ABSENT);
                    long late = countStatus(entries, AttendanceStatus.LATE);
                    long excused = countStatus(entries, AttendanceStatus.EXCUSED);
                    return new MonthlyAttendanceResponse.StudentAttendanceRow(
                            student.getId(),
                            student.getFullName(),
                            entries,
                            new MonthlyAttendanceResponse.AttendanceSummary(present, absent, late, excused));
                })
                .toList();

        return new MonthlyAttendanceResponse(
                classRoom.getId(),
                classRoom.getName(),
                year,
                month,
                sessionDates.stream().map(LocalDate::toString).toList(),
                rows);
    }

    private long countStatus(Map<String, MonthlyAttendanceResponse.AttendanceEntry> entries, AttendanceStatus status) {
        return entries.values().stream().filter(e -> e.status() == status).count();
    }
}
