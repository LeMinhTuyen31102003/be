package be.spring.vanconhung.controller;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.spring.vanconhung.dto.MonthlyTuitionResponse;
import be.spring.vanconhung.dto.UpdateTuitionRequest;
import be.spring.vanconhung.entity.ClassRoom;
import be.spring.vanconhung.entity.Role;
import be.spring.vanconhung.entity.Tuition;
import be.spring.vanconhung.entity.TuitionStatus;
import be.spring.vanconhung.entity.User;
import be.spring.vanconhung.repository.ClassRoomRepository;
import be.spring.vanconhung.repository.TuitionRepository;
import be.spring.vanconhung.service.TuitionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classes/{classId}/tuition")
@Transactional
public class TuitionController {

    private final ClassRoomRepository classRoomRepository;
    private final TuitionRepository tuitionRepository;
    private final TuitionService tuitionService;

    public TuitionController(ClassRoomRepository classRoomRepository, TuitionRepository tuitionRepository,
            TuitionService tuitionService) {
        this.classRoomRepository = classRoomRepository;
        this.tuitionRepository = tuitionRepository;
        this.tuitionService = tuitionService;
    }

    @GetMapping
    public ResponseEntity<MonthlyTuitionResponse> monthly(@PathVariable Long classId,
            @RequestParam int year, @RequestParam int month) {
        return classRoomRepository.findById(classId)
                .map(classRoom -> ResponseEntity.ok(buildMonthlyResponse(classRoom, year, month)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<MonthlyTuitionResponse> update(@PathVariable Long classId, @PathVariable Long studentId,
            @RequestParam int year, @RequestParam int month,
            @Valid @RequestBody UpdateTuitionRequest request) {
        Optional<ClassRoom> classRoomOpt = classRoomRepository.findById(classId);
        if (classRoomOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ClassRoom classRoom = classRoomOpt.get();
        User student = classRoom.getStudents().stream()
                .filter(s -> s.getId().equals(studentId) && s.getRole() == Role.STUDENT)
                .findFirst()
                .orElse(null);
        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        Tuition tuition = tuitionRepository
                .findByClassRoom_IdAndStudent_IdAndYearAndMonth(classId, studentId, year, month)
                .orElseGet(Tuition::new);
        TuitionStatus previousStatus = tuition.getStatus();
        if (previousStatus == TuitionStatus.PAID && request.status() != TuitionStatus.PAID) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        if (tuition.getId() == null) {
            tuition.setClassRoom(classRoom);
            tuition.setStudent(student);
            tuition.setYear(year);
            tuition.setMonth(month);
        }
        tuition.setAmount(request.amount());
        tuition.setNote(request.note());
        tuition.setStatus(request.status());
        if (request.status() == TuitionStatus.PAID && previousStatus != TuitionStatus.PAID) {
            tuition.setPaidAt(Instant.now());
        } else if (request.status() != TuitionStatus.PAID) {
            tuition.setPaidAt(null);
        }
        if (request.status() != TuitionStatus.PENDING) {
            tuition.setRequestedAt(null);
        }
        tuitionRepository.save(tuition);

        return ResponseEntity.ok(buildMonthlyResponse(classRoom, year, month));
    }

    private MonthlyTuitionResponse buildMonthlyResponse(ClassRoom classRoom, int year, int month) {
        Map<Long, Integer> sessionCounts = tuitionService.countBillableSessionsByStudent(classRoom.getId(), year,
                month);

        List<MonthlyTuitionResponse.StudentTuitionRow> rows = classRoom.getStudents().stream()
                .sorted(Comparator.comparing(User::getFullName))
                .map(student -> {
                    int sessionCount = sessionCounts.getOrDefault(student.getId(), 0);
                    TuitionService.StudentTuitionInfo info = tuitionService.computeForStudent(classRoom, student,
                            sessionCount, year, month);
                    return new MonthlyTuitionResponse.StudentTuitionRow(
                            student.getId(), student.getFullName(), info.sessionCount(), info.amount(),
                            info.status(), info.requestedAt(), info.paidAt(), info.note());
                })
                .toList();

        int paidCount = (int) rows.stream()
                .filter(r -> r.status() == TuitionStatus.PAID)
                .count();
        int pendingCount = (int) rows.stream()
                .filter(r -> r.status() == TuitionStatus.PENDING)
                .count();
        long totalDue = rows.stream().mapToLong(MonthlyTuitionResponse.StudentTuitionRow::amount).sum();
        long totalCollected = rows.stream()
                .filter(r -> r.status() == TuitionStatus.PAID)
                .mapToLong(MonthlyTuitionResponse.StudentTuitionRow::amount)
                .sum();

        MonthlyTuitionResponse.TuitionSummary summary = new MonthlyTuitionResponse.TuitionSummary(
                rows.size(), paidCount, pendingCount, totalDue, totalCollected, totalDue - totalCollected);

        return new MonthlyTuitionResponse(
                classRoom.getId(), classRoom.getName(), classRoom.getFeePerSession(), year, month, rows, summary);
    }
}
