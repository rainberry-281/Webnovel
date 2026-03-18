package com.bn.berrynovel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.bn.berrynovel.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    void deleteById(Long id);

    User findByUsername(String username);

    User findByEmail(String email);

    User findFirstById(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findAll(Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCase(String keyword, Pageable pageable);

    List<User> findByStatus(boolean status);
}