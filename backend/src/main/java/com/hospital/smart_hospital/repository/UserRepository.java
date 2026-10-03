package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE UPPER(u.role) = UPPER(:role)")
    List<User> findUsersByRole(@Param("role") String role);

    @Query("""
           SELECT u FROM User u
           WHERE UPPER(u.role) = UPPER(:role)
           AND UPPER(u.work_area) = UPPER(:workArea)
           """)
    List<User> findUsersByRoleAndWorkArea(
            @Param("role") String role,
            @Param("workArea") String workArea
    );
}