package be.spring.vanconhung.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import be.spring.vanconhung.entity.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByClassRoom_IdAndDateBetween(Long classId, LocalDate start, LocalDate end);

    Optional<Attendance> findByClassRoom_IdAndStudent_IdAndDate(Long classId, Long studentId, LocalDate date);

    void deleteByStudent_Id(Long studentId);
}
