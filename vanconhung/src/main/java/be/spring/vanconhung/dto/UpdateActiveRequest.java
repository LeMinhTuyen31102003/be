package be.spring.vanconhung.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateActiveRequest(@NotNull Boolean active) {
}
