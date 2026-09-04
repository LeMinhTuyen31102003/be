package be.spring.vanconhung.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import be.spring.vanconhung.dto.ClassConflictResponse;
import be.spring.vanconhung.dto.ClassDetailResponse;
import be.spring.vanconhung.dto.ClassResponse;
import be.spring.vanconhung.dto.CreateClassRequest;
import be.spring.vanconhung.dto.ScheduleSlotInput;
import be.spring.vanconhung.dto.UpdateActiveRequest;
import be.spring.vanconhung.dto.UpdateClassRequest;
import be.spring.vanconhung.entity.ClassRoom;
import be.spring.vanconhung.entity.ClassSchedule;
import be.spring.vanconhung.entity.Role;
import be.spring.vanconhung.entity.User;
import be.spring.vanconhung.repository.ClassRoomRepository;
import be.spring.vanconhung.repository.UserRepository;
import be.spring.vanconhung.util.SortingUtils;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classes")
@Transactional
public class ClassController {

    private final ClassRoomRepository classRoomRepository;
    private final UserRepository userRepository;

    public ClassController(ClassRoomRepository classRoomRepository, UserRepository userRepository) {
        this.classRoomRepository = classRoomRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ClassResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) Long assignableToStudentId,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        List<ClassRoom> rooms = classRoomRepository.search(search == null ? null : search.trim());

        if ("active".equalsIgnoreCase(status)) {
            rooms = rooms.stream().filter(ClassRoom::isActive).toList();
        } else if ("inactive".equalsIgnoreCase(status)) {
            rooms = rooms.stream().filter(c -> !c.isActive()).toList();
        }

        if (assignableToStudentId != null) {
            List<ClassRoom> studentClasses = classRoomRepository.findByStudents_Id(assignableToStudentId);
            boolean hasOtherActiveClass = studentClasses.stream().anyMatch(ClassRoom::isActive);
            Set<Long> memberClassIds = studentClasses.stream().map(ClassRoom::getId).collect(Collectors.toSet());
            rooms = rooms.stream()
                    .filter(c -> !memberClassIds.contains(c.getId()))
                    .filter(c -> !hasOtherActiveClass)
                    .toList();
        }

        Comparator<ClassRoom> comparator = classComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return rooms.stream()
                .sorted(comparator)
                .map(ClassResponse::from)
                .toList();
    }

    private Comparator<ClassRoom> classComparator(String sortBy) {
        Comparator<ClassRoom> byName = (a, b) -> SortingUtils.VI_COLLATOR.compare(
                SortingUtils.nullToEmpty(a.getName()), SortingUtils.nullToEmpty(b.getName()));
        Comparator<ClassRoom> byGrade = (a, b) -> SortingUtils.compareGrade(a.getGrade(), b.getGrade());
        Comparator<ClassRoom> byStudentCount = Comparator.comparingInt(c -> c.getStudents().size());
        Comparator<ClassRoom> byActive = Comparator.comparing(ClassRoom::isActive);

        return switch (sortBy) {
            case "grade" -> byGrade;
            case "studentCount" -> byStudentCount;
            case "active" -> byActive;
            default -> byName;
        };
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassDetailResponse> detail(@PathVariable Long id) {
        return classRoomRepository.findById(id)
                .map(classRoom -> ResponseEntity.ok(ClassDetailResponse.from(classRoom, classRoomRepository)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClassResponse> create(@Valid @RequestBody CreateClassRequest request) {
        ClassRoom classRoom = new ClassRoom();
        classRoom.setName(request.name());
        classRoom.setGrade(request.grade());
        classRoom.setNote(request.note());
        classRoom.setFeePerSession(request.feePerSession());
        applySchedules(classRoom, request.schedules());

        ClassRoom saved = classRoomRepository.save(classRoom);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassResponse> update(@PathVariable Long id,
            @Valid @RequestBody UpdateClassRequest request) {
        return findClass(id)
                .map(classRoom -> {
                    classRoom.setName(request.name());
                    classRoom.setGrade(request.grade());
                    classRoom.setNote(request.note());
                    classRoom.setFeePerSession(request.feePerSession());
                    applySchedules(classRoom, request.schedules());
                    return ResponseEntity.ok(ClassResponse.from(classRoomRepository.save(classRoom)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void applySchedules(ClassRoom classRoom, List<ScheduleSlotInput> slots) {
        classRoom.getSchedules().clear();
        if (slots == null) {
            return;
        }
        for (ScheduleSlotInput slot : slots) {
            ClassSchedule schedule = new ClassSchedule();
            schedule.setClassRoom(classRoom);
            schedule.setDayOfWeek(slot.dayOfWeek());
            schedule.setStartTime(slot.startTime());
            schedule.setEndTime(slot.endTime());
            classRoom.getSchedules().add(schedule);
        }
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ClassResponse> updateActive(@PathVariable Long id,
            @Valid @RequestBody UpdateActiveRequest request) {
        return findClass(id)
                .map(classRoom -> {
                    classRoom.setActive(request.active());
                    return ResponseEntity.ok(ClassResponse.from(classRoomRepository.save(classRoom)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return findClass(id)
                .map(classRoom -> {
                    classRoomRepository.delete(classRoom);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/students/{studentId}")
    public ResponseEntity<?> addStudent(@PathVariable Long id, @PathVariable Long studentId) {
        Optional<ClassRoom> classRoom = findClass(id);
        Optional<User> student = findStudent(studentId);
        if (classRoom.isEmpty() || student.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClassRoom room = classRoom.get();
        User user = student.get();

        boolean alreadyInThisClass = room.getStudents().stream().anyMatch(s -> s.getId().equals(studentId));
        if (!alreadyInThisClass) {
            Optional<ClassRoom> otherActiveClass = classRoomRepository.findByStudents_Id(studentId).stream()
                    .filter(ClassRoom::isActive)
                    .filter(c -> !c.getId().equals(id))
                    .findFirst();
            if (otherActiveClass.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ClassConflictResponse(otherActiveClass.get().getName()));
            }
        }

        room.getStudents().add(user);
        if (!user.isEnabled()) {
            user.setEnabled(true);
            userRepository.save(user);
        }

        return ResponseEntity.ok(ClassDetailResponse.from(classRoomRepository.save(room), classRoomRepository));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public ResponseEntity<ClassDetailResponse> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        return findClass(id)
                .map(classRoom -> {
                    classRoom.getStudents().removeIf(s -> s.getId().equals(studentId));
                    return ResponseEntity.ok(ClassDetailResponse.from(classRoomRepository.save(classRoom), classRoomRepository));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Optional<ClassRoom> findClass(Long id) {
        return classRoomRepository.findById(id);
    }

    private Optional<User> findStudent(Long id) {
        return userRepository.findById(id).filter(u -> u.getRole() == Role.STUDENT);
    }
}
