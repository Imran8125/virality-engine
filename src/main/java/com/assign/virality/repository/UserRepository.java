package com.assign.virality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.assign.virality.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
