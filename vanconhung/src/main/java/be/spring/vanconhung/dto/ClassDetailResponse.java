package be.spring.vanconhung.dto;

import java.util.Comparator;
import java.util.List;

import be.spring.vanconhung.entity.ClassRoom;

public record ClassDetailResponse(
        Long id,
        String name,
        String grade,
        List<ScheduleSlotResponse> schedules,
        String note,
        boolean active,
        List<StudentResponse> students) {

    public static ClassDetailResponse from(ClassRoom classRoom) {
        List<StudentResponse> students = classRoom.getStudents().stream()
                .map(StudentResponse::from)
                .sorted(Comparator.comparing(StudentResponse::fullName))
                .toList();

        return new ClassDetailResponse(
                classRoom.getId(),
                classRoom.getName(),
                classRoom.getGrade(),
                classRoom.getSchedules().stream().map(ScheduleSlotResponse::from).toList(),
                classRoom.getNote(),
                classRoom.isActive(),
                students);
    }
}
