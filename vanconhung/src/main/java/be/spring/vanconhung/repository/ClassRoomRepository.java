package be.spring.vanconhung.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import be.spring.vanconhung.entity.ClassRoom;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

    List<ClassRoom> findByStudents_Id(Long studentId);
}
