package be.spring.vanconhung.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import be.spring.vanconhung.entity.ClassSchedule;

public record ScheduleSlotResponse(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime) {

    public static ScheduleSlotResponse from(ClassSchedule slot) {
        return new ScheduleSlotResponse(
                slot.getId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime());
    }
}
