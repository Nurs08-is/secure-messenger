package kz.securemessenger.server.repository;

import kz.securemessenger.server.model.User;
import kz.securemessenger.common.model.UserStatus;
import kz.securemessenger.common.model.Role;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByStatus(UserStatus status);

    List<User> findByRole(Role role);

    List<User> findByDepartment_Id(Long departmentId);

    List<User> findByDepartment_IdAndStatus(
            Long departmentId, UserStatus status);

    List<User> findByStatusAndRole(
            UserStatus status, Role role);
}