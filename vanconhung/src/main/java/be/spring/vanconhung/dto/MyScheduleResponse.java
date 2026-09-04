package be.spring.vanconhung.dto;

import java.util.List;

public record MyScheduleResponse(List<ClassScheduleGroup> classes) {

    public record ClassScheduleGroup(Long classId, String className, List<ScheduleSlotResponse> slots) {
    }
}
