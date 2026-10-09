package com.fintry.repository;

import com.fintry.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.Query(value="select * from users where lower(btrim(email)) = :email", nativeQuery=true)
    java.util.Optional<User> findByEmailIgnoreCase(String email);
}
