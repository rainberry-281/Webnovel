package com.bn.berrynovel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.bn.berrynovel.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    void deleteById(int id);

    User findByUsername(String username);

    User findFirstById(int id);

    Page<User> findAll(Pageable pageable);

    List<User> findByStatus(boolean status);
}