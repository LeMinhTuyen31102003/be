package be.spring.vanconhung.dto;

import be.spring.vanconhung.entity.BankSettings;

public record BankSettingsResponse(
        String bankId,
        String bankName,
        String accountNumber,
        String accountName,
        boolean configured) {

    public static BankSettingsResponse from(BankSettings settings) {
        boolean configured = isFilled(settings.getBankId())
                && isFilled(settings.getAccountNumber())
                && isFilled(settings.getAccountName());

        return new BankSettingsResponse(
                settings.getBankId(),
                settings.getBankName(),
                settings.getAccountNumber(),
                settings.getAccountName(),
                configured);
    }

    private static boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }
}
