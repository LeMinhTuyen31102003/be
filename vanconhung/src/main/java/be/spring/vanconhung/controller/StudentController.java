package be.spring.vanconhung.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import be.spring.vanconhung.dto.CreateStudentRequest;
import be.spring.vanconhung.dto.StudentResponse;
import be.spring.vanconhung.dto.UpdateActiveRequest;
import be.spring.vanconhung.dto.UpdateStudentRequest;
import be.spring.vanconhung.entity.ClassRoom;
import be.spring.vanconhung.entity.Role;
import be.spring.vanconhung.entity.User;
import be.spring.vanconhung.repository.AttendanceRepository;
import be.spring.vanconhung.repository.ClassRoomRepository;
import be.spring.vanconhung.repository.TuitionRepository;
import be.spring.vanconhung.repository.UserRepository;
import be.spring.vanconhung.util.SortingUtils;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/students")
@Transactional
public class StudentController {

    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;
    private final AttendanceRepository attendanceRepository;
    private final TuitionRepository tuitionRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentController(UserRepository userRepository, ClassRoomRepository classRoomRepository,
            AttendanceRepository attendanceRepository, TuitionRepository tuitionRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.classRoomRepository = classRoomRepository;
        this.attendanceRepository = attendanceRepository;
        this.tuitionRepository = tuitionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<StudentResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long assignableToClassId,
            @RequestParam(required = false, defaultValue = "fullName") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        List<User> students = userRepository.searchByRole(Role.STUDENT,
                search == null ? null : search.trim());

        if ("active".equalsIgnoreCase(status)) {
            students = students.stream().filter(User::isEnabled).toList();
        } else if ("inactive".equalsIgnoreCase(status)) {
            students = students.stream().filter(u -> !u.isEnabled()).toList();
        }

        if (classId != null) {
            Set<Long> memberIds = classRoomRepository.findById(classId)
                    .map(c -> c.getStudents().stream().map(User::getId).collect(Collectors.toSet()))
                    .orElseGet(Set::of);
            students = students.stream().filter(u -> memberIds.contains(u.getId())).toList();
        }

        if (assignableToClassId != null) {
            Set<Long> memberIds = classRoomRepository.findById(assignableToClassId)
                    .map(c -> c.getStudents().stream().map(User::getId).collect(Collectors.toSet()))
                    .orElseGet(Set::of);
            students = students.stream()
                    .filter(u -> !memberIds.contains(u.getId()))
                    .filter(u -> classRoomRepository.findByStudents_Id(u.getId()).stream()
                            .noneMatch(c -> c.isActive() && !c.getId().equals(assignableToClassId)))
                    .toList();
        }

        Comparator<User> comparator = studentComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return students.stream()
                .sorted(comparator)
                .map(this::toResponse)
                .toList();
    }

    private Comparator<User> studentComparator(String sortBy) {
        Comparator<User> byFullName = (a, b) -> SortingUtils.VI_COLLATOR.compare(
                SortingUtils.nullToEmpty(a.getFullName()), SortingUtils.nullToEmpty(b.getFullName()));
        Comparator<User> byUsername = Comparator.comparing(u -> SortingUtils.nullToEmpty(u.getUsername()));
        Comparator<User> byGrade = (a, b) -> SortingUtils.compareGrade(a.getGrade(), b.getGrade());
        Comparator<User> byActive = Comparator.comparing(User::isEnabled);
        Comparator<User> byCreatedAt = Comparator.comparing(User::getCreatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder()));

        return switch (sortBy) {
            case "grade" -> byGrade;
            case "username" -> byUsername;
            case "active" -> byActive;
            case "createdAt" -> byCreatedAt;
            default -> byFullName;
        };
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody CreateStudentRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User student = new User();
        student.setUsername(request.username());
        student.setPassword(passwordEncoder.encode(request.password()));
        student.setFullName(request.fullName());
        student.setGrade(request.grade());
        student.setSchoolName(request.schoolName());
        student.setParentName(request.parentName());
        student.setParentPhone(request.parentPhone());
        student.setRole(Role.STUDENT);
        student.setEnabled(Boolean.TRUE.equals(request.active()));

        User saved = userRepository.save(student);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> update(@PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        return findStudent(id)
                .map(student -> {
                    student.setFullName(request.fullName());
                    student.setGrade(request.grade());
                    student.setSchoolName(request.schoolName());
                    student.setParentName(request.parentName());
                    student.setParentPhone(request.parentPhone());
                    return ResponseEntity.ok(toResponse(userRepository.save(student)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<StudentResponse> updateActive(@PathVariable Long id,
            @Valid @RequestBody UpdateActiveRequest request) {
        return findStudent(id)
                .map(student -> {
                    student.setEnabled(request.active());
                    return ResponseEntity.ok(toResponse(userRepository.save(student)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return findStudent(id)
                .map(student -> {
                    for (ClassRoom classRoom : classRoomRepository.findByStudents_Id(id)) {
                        classRoom.getStudents().removeIf(s -> s.getId().equals(id));
                        classRoomRepository.save(classRoom);
                    }
                    attendanceRepository.deleteByStudent_Id(id);
                    tuitionRepository.deleteByStudent_Id(id);
                    userRepository.delete(student);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Optional<User> findStudent(Long id) {
        return userRepository.findById(id).filter(student -> student.getRole() == Role.STUDENT);
    }

    private StudentResponse toResponse(User user) {
        return StudentResponse.from(user, classRoomRepository.findByStudents_Id(user.getId()));
    }
}
