package be.spring.vanconhung.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import be.spring.vanconhung.entity.ClassRoom;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

    List<ClassRoom> findByStudents_Id(Long studentId);

    @Query("SELECT c FROM ClassRoom c WHERE (:search IS NULL OR :search = '' "
            + "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<ClassRoom> search(@Param("search") String search);
}
