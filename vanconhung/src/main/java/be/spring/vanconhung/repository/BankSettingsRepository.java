package be.spring.vanconhung.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import be.spring.vanconhung.entity.BankSettings;

public interface BankSettingsRepository extends JpaRepository<BankSettings, Long> {
}
