package be.spring.vanconhung.dto;

import java.util.List;

import be.spring.vanconhung.entity.ClassRoom;

public record ClassResponse(
        Long id,
        String name,
        String grade,
        List<ScheduleSlotResponse> schedules,
        String note,
        Long feePerSession,
        boolean active,
        int studentCount) {

    public static ClassResponse from(ClassRoom classRoom) {
        return new ClassResponse(
                classRoom.getId(),
                classRoom.getName(),
                classRoom.getGrade(),
                classRoom.getSchedules().stream().map(ScheduleSlotResponse::from).toList(),
                classRoom.getNote(),
                classRoom.getFeePerSession(),
                classRoom.isActive(),
                classRoom.getStudents().size());
    }
}
