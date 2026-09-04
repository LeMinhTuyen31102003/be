package be.spring.vanconhung.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import be.spring.vanconhung.entity.Role;
import be.spring.vanconhung.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role "
            + "AND (:search IS NULL OR :search = '' "
            + "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> searchByRole(@Param("role") Role role, @Param("search") String search);
}
