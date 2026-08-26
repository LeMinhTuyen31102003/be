package be.spring.vanconhung.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import be.spring.vanconhung.entity.Role;
import be.spring.vanconhung.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(Role role);
}
