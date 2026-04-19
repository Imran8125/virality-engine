package com.assign.virality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.assign.virality.domain.Bot;

@Repository
public interface BotRepository extends JpaRepository<Bot, Long> {
}
