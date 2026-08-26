package be.spring.vanconhung.controller;

import java.util.List;
import java.util.Optional;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import be.spring.vanconhung.dto.CreateStudentRequest;
import be.spring.vanconhung.dto.StudentResponse;
import be.spring.vanconhung.dto.UpdateActiveRequest;
import be.spring.vanconhung.dto.UpdateStudentRequest;
import be.spring.vanconhung.entity.ClassRoom;
import be.spring.vanconhung.entity.Role;
import be.spring.vanconhung.entity.User;
import be.spring.vanconhung.repository.ClassRoomRepository;
import be.spring.vanconhung.repository.UserRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/students")
@Transactional
public class StudentController {

    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentController(UserRepository userRepository, ClassRoomRepository classRoomRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.classRoomRepository = classRoomRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<StudentResponse> list() {
        return userRepository.findByRole(Role.STUDENT).stream()
                .map(this::toResponse)
                .toList();
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
