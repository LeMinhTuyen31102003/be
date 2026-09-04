package be.spring.vanconhung.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import be.spring.vanconhung.entity.Tuition;

public interface TuitionRepository extends JpaRepository<Tuition, Long> {

    List<Tuition> findByClassRoom_IdAndYearAndMonth(Long classId, int year, int month);

    Optional<Tuition> findByClassRoom_IdAndStudent_IdAndYearAndMonth(Long classId, Long studentId, int year,
            int month);

    void deleteByStudent_Id(Long studentId);
}
