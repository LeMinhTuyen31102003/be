package be.spring.vanconhung.service;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import be.spring.vanconhung.entity.AttendanceStatus;
import be.spring.vanconhung.entity.ClassRoom;
import be.spring.vanconhung.entity.Tuition;
import be.spring.vanconhung.entity.TuitionStatus;
import be.spring.vanconhung.entity.User;
import be.spring.vanconhung.repository.AttendanceRepository;
import be.spring.vanconhung.repository.TuitionRepository;

@Service
public class TuitionService {

    private static final Set<AttendanceStatus> BILLABLE_STATUSES = Set.of(AttendanceStatus.PRESENT,
            AttendanceStatus.LATE);

    private final AttendanceRepository attendanceRepository;
    private final TuitionRepository tuitionRepository;

    public TuitionService(AttendanceRepository attendanceRepository, TuitionRepository tuitionRepository) {
        this.attendanceRepository = attendanceRepository;
        this.tuitionRepository = tuitionRepository;
    }

    public record StudentTuitionInfo(long amount, int sessionCount, TuitionStatus status, Instant requestedAt,
            Instant paidAt, String note) {
    }

    public Map<Long, Integer> countBillableSessionsByStudent(Long classId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return attendanceRepository
                .findByClassRoom_IdAndDateBetween(classId, yearMonth.atDay(1), yearMonth.atEndOfMonth())
                .stream()
                .filter(a -> BILLABLE_STATUSES.contains(a.getStatus()))
                .collect(Collectors.groupingBy(a -> a.getStudent().getId(), Collectors.summingInt(a -> 1)));
    }

    public StudentTuitionInfo computeForStudent(ClassRoom classRoom, User student, int sessionCount, int year,
            int month) {
        return tuitionRepository
                .findByClassRoom_IdAndStudent_IdAndYearAndMonth(classRoom.getId(), student.getId(), year, month)
                .map(t -> new StudentTuitionInfo(t.getAmount(), sessionCount, t.getStatus(), t.getRequestedAt(),
                        t.getPaidAt(), t.getNote()))
                .orElseGet(() -> new StudentTuitionInfo(defaultAmount(classRoom, sessionCount), sessionCount,
                        TuitionStatus.UNPAID, null, null, null));
    }

    /**
     * A student confirms they have transferred the money; moves the record to
     * PENDING so the teacher can verify and mark it PAID.
     */
    public StudentTuitionInfo requestPaymentConfirmation(ClassRoom classRoom, User student, int year, int month) {
        int sessionCount = countBillableSessionsByStudent(classRoom.getId(), year, month)
                .getOrDefault(student.getId(), 0);

        Tuition tuition = tuitionRepository
                .findByClassRoom_IdAndStudent_IdAndYearAndMonth(classRoom.getId(), student.getId(), year, month)
                .orElseGet(Tuition::new);

        if (tuition.getStatus() == TuitionStatus.PAID) {
            throw new IllegalStateException("Học phí đã được xác nhận thanh toán.");
        }

        if (tuition.getId() == null) {
            tuition.setClassRoom(classRoom);
            tuition.setStudent(student);
            tuition.setYear(year);
            tuition.setMonth(month);
            tuition.setAmount(defaultAmount(classRoom, sessionCount));
        }
        tuition.setStatus(TuitionStatus.PENDING);
        tuition.setRequestedAt(Instant.now());
        tuition.setNote(null);
        tuitionRepository.save(tuition);

        return new StudentTuitionInfo(tuition.getAmount(), sessionCount, tuition.getStatus(),
                tuition.getRequestedAt(), tuition.getPaidAt(), tuition.getNote());
    }

    public long defaultAmount(ClassRoom classRoom, int sessionCount) {
        long feePerSession = classRoom.getFeePerSession() != null ? classRoom.getFeePerSession() : 0L;
        return feePerSession * sessionCount;
    }
}
