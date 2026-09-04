package be.spring.vanconhung.dto;

public record UpdateBankSettingsRequest(
        String bankId,
        String bankName,
        String accountNumber,
        String accountName) {
}
