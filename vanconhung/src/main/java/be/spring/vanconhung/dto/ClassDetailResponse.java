package be.spring.vanconhung.dto;

import java.util.Comparator;
import java.util.List;

import be.spring.vanconhung.entity.ClassRoom;
import be.spring.vanconhung.repository.ClassRoomRepository;

public record ClassDetailResponse(
        Long id,
        String name,
        String grade,
        List<ScheduleSlotResponse> schedules,
        String note,
        Long feePerSession,
        boolean active,
        List<StudentResponse> students) {

    public static ClassDetailResponse from(ClassRoom classRoom, ClassRoomRepository classRoomRepository) {
        List<StudentResponse> students = classRoom.getStudents().stream()
                .map(student -> StudentResponse.from(student, classRoomRepository.findByStudents_Id(student.getId())))
                .sorted(Comparator.comparing(StudentResponse::fullName))
                .toList();

        return new ClassDetailResponse(
                classRoom.getId(),
                classRoom.getName(),
                classRoom.getGrade(),
                classRoom.getSchedules().stream().map(ScheduleSlotResponse::from).toList(),
                classRoom.getNote(),
                classRoom.getFeePerSession(),
                classRoom.isActive(),
                students);
    }
}
