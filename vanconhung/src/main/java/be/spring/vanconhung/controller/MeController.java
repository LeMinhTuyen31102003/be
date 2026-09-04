package be.spring.vanconhung.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.spring.vanconhung.dto.ClassmateResponse;
import be.spring.vanconhung.dto.MyAttendanceResponse;
import be.spring.vanconhung.dto.MyClassmatesResponse;
import be.spring.vanconhung.dto.MyProfileResponse;
import be.spring.vanconhung.dto.MyScheduleResponse;
import be.spring.vanconhung.dto.MyTuitionResponse;
import be.spring.vanconhung.dto.ScheduleSlotResponse;
import be.spring.vanconhung.dto.TeacherContactResponse;
import be.spring.vanconhung.dto.UpdateMyProfileRequest;
import be.spring.vanconhung.entity.Attendance;
import be.spring.vanconhung.entity.AttendanceStatus;
import be.spring.vanconhung.entity.ClassRoom;
import be.spring.vanconhung.entity.Role;
import be.spring.vanconhung.entity.User;
import be.spring.vanconhung.repository.AttendanceRepository;
import be.spring.vanconhung.repository.ClassRoomRepository;
import be.spring.vanconhung.repository.UserRepository;
import be.spring.vanconhung.service.TuitionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/me")
@Transactional
public class MeController {

    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;
    private final AttendanceRepository attendanceRepository;
    private final TuitionService tuitionService;

    public MeController(UserRepository userRepository, ClassRoomRepository classRoomRepository,
            AttendanceRepository attendanceRepository, TuitionService tuitionService) {
        this.userRepository = userRepository;
        this.classRoomRepository = classRoomRepository;
        this.attendanceRepository = attendanceRepository;
        this.tuitionService = tuitionService;
    }

    @GetMapping("/profile")
    public MyProfileResponse myProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return MyProfileResponse.from(currentUser(userDetails));
    }

    @PutMapping("/profile")
    public MyProfileResponse updateMyProfile(@AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateMyProfileRequest request) {
        User me = currentUser(userDetails);
        me.setFullName(request.fullName());
        me.setEmail(request.email());
        me.setPhone(request.phone());
        me.setAddress(request.address());
        me.setGrade(request.grade());
        me.setSchoolName(request.schoolName());
        me.setParentName(request.parentName());
        me.setParentPhone(request.parentPhone());
        userRepository.save(me);
        return MyProfileResponse.from(me);
    }

    @GetMapping("/schedule")
    public MyScheduleResponse mySchedule(@AuthenticationPrincipal UserDetails userDetails) {
        User me = currentUser(userDetails);
        List<ClassRoom> classes = classRoomRepository.findByStudents_Id(me.getId());

        List<MyScheduleResponse.ClassScheduleGroup> groups = classes.stream()
                .map(classRoom -> new MyScheduleResponse.ClassScheduleGroup(
                        classRoom.getId(),
                        classRoom.getName(),
                        classRoom.getSchedules().stream().map(ScheduleSlotResponse::from).toList()))
                .toList();

        return new MyScheduleResponse(groups);
    }

    @GetMapping("/classmates")
    public MyClassmatesResponse myClassmates(@AuthenticationPrincipal UserDetails userDetails) {
        User me = currentUser(userDetails);
        List<ClassRoom> classes = classRoomRepository.findByStudents_Id(me.getId());

        List<MyClassmatesResponse.ClassGroup> groups = classes.stream()
                .map(classRoom -> new MyClassmatesResponse.ClassGroup(
                        classRoom.getId(),
                        classRoom.getName(),
                        classRoom.getStudents().stream()
                                .filter(s -> !s.getId().equals(me.getId()))
                                .sorted(Comparator.comparing(User::getFullName,
                                        Comparator.nullsLast(String::compareTo)))
                                .map(ClassmateResponse::from)
                                .toList()))
                .toList();

        return new MyClassmatesResponse(groups);
    }

    @GetMapping("/teacher")
    public ResponseEntity<TeacherContactResponse> myTeacher() {
        return userRepository.findByRole(Role.TEACHER).stream()
                .findFirst()
                .map(TeacherContactResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/tuition")
    public MyTuitionResponse myTuition(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int year, @RequestParam int month) {
        User me = currentUser(userDetails);
        List<ClassRoom> classes = classRoomRepository.findByStudents_Id(me.getId());

        List<MyTuitionResponse.ClassTuition> rows = classes.stream()
                .map(classRoom -> {
                    Map<Long, Integer> sessionCounts = tuitionService
                            .countBillableSessionsByStudent(classRoom.getId(), year, month);
                    int sessionCount = sessionCounts.getOrDefault(me.getId(), 0);
                    TuitionService.StudentTuitionInfo info = tuitionService.computeForStudent(classRoom, me,
                            sessionCount, year, month);
                    long feePerSession = classRoom.getFeePerSession() != null ? classRoom.getFeePerSession() : 0L;
                    return new MyTuitionResponse.ClassTuition(
                            classRoom.getId(), classRoom.getName(), feePerSession, info.sessionCount(),
                            info.amount(), info.status(), info.requestedAt(), info.paidAt(), info.note());
                })
                .toList();

        return new MyTuitionResponse(year, month, rows);
    }

    @PutMapping("/tuition/{classId}/confirm-payment")
    public ResponseEntity<MyTuitionResponse.ClassTuition> confirmPayment(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long classId,
            @RequestParam int year, @RequestParam int month) {
        User me = currentUser(userDetails);
        ClassRoom classRoom = classRoomRepository.findById(classId).orElse(null);
        if (classRoom == null || classRoom.getStudents().stream().noneMatch(s -> s.getId().equals(me.getId()))) {
            return ResponseEntity.notFound().build();
        }

        TuitionService.StudentTuitionInfo info = tuitionService.requestPaymentConfirmation(classRoom, me, year,
                month);
        long feePerSession = classRoom.getFeePerSession() != null ? classRoom.getFeePerSession() : 0L;
        return ResponseEntity.ok(new MyTuitionResponse.ClassTuition(
                classRoom.getId(), classRoom.getName(), feePerSession, info.sessionCount(), info.amount(),
                info.status(), info.requestedAt(), info.paidAt(), info.note()));
    }

    @GetMapping("/attendance")
    public MyAttendanceResponse myAttendance(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int year, @RequestParam int month) {
        User me = currentUser(userDetails);
        List<ClassRoom> classes = classRoomRepository.findByStudents_Id(me.getId());
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<MyAttendanceResponse.ClassAttendance> rows = classes.stream()
                .map(classRoom -> {
                    List<Attendance> attendances = attendanceRepository
                            .findByClassRoom_IdAndDateBetween(classRoom.getId(), start, end)
                            .stream()
                            .filter(a -> a.getStudent().getId().equals(me.getId()))
                            .sorted(Comparator.comparing(Attendance::getDate))
                            .toList();

                    List<MyAttendanceResponse.SessionEntry> sessions = attendances.stream()
                            .map(a -> new MyAttendanceResponse.SessionEntry(a.getDate().toString(), a.getStatus()))
                            .toList();

                    long present = countStatus(attendances, AttendanceStatus.PRESENT);
                    long absent = countStatus(attendances, AttendanceStatus.ABSENT);
                    long late = countStatus(attendances, AttendanceStatus.LATE);
                    long excused = countStatus(attendances, AttendanceStatus.EXCUSED);

                    return new MyAttendanceResponse.ClassAttendance(classRoom.getId(), classRoom.getName(), sessions,
                            new MyAttendanceResponse.AttendanceSummary(present, absent, late, excused));
                })
                .toList();

        return new MyAttendanceResponse(year, month, rows);
    }

    private long countStatus(List<Attendance> attendances, AttendanceStatus status) {
        return attendances.stream().filter(a -> a.getStatus() == status).count();
    }

    private User currentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
