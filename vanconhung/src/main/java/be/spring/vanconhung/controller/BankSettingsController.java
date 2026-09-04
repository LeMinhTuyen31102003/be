package be.spring.vanconhung.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.spring.vanconhung.dto.BankSettingsResponse;
import be.spring.vanconhung.dto.UpdateBankSettingsRequest;
import be.spring.vanconhung.entity.BankSettings;
import be.spring.vanconhung.repository.BankSettingsRepository;

@RestController
@RequestMapping("/api/settings/bank")
public class BankSettingsController {

    private static final Long SETTINGS_ID = 1L;

    private final BankSettingsRepository bankSettingsRepository;

    public BankSettingsController(BankSettingsRepository bankSettingsRepository) {
        this.bankSettingsRepository = bankSettingsRepository;
    }

    @GetMapping
    public BankSettingsResponse get() {
        return BankSettingsResponse.from(loadOrCreate());
    }

    @PutMapping
    public BankSettingsResponse update(@RequestBody UpdateBankSettingsRequest request) {
        BankSettings settings = loadOrCreate();
        settings.setBankId(request.bankId());
        settings.setBankName(request.bankName());
        settings.setAccountNumber(request.accountNumber());
        settings.setAccountName(request.accountName());
        return BankSettingsResponse.from(bankSettingsRepository.save(settings));
    }

    private BankSettings loadOrCreate() {
        return bankSettingsRepository.findById(SETTINGS_ID).orElseGet(() -> {
            BankSettings settings = new BankSettings();
            settings.setId(SETTINGS_ID);
            return settings;
        });
    }
}
