package be.spring.vanconhung.dto;

public record LoginResponse(
        String token,
        String userName,
        String role) {
}
