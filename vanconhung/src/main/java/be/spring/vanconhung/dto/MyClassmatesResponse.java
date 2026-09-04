package be.spring.vanconhung.dto;

import java.util.List;

public record MyClassmatesResponse(List<ClassGroup> classes) {

    public record ClassGroup(Long classId, String className, List<ClassmateResponse> classmates) {
    }
}
