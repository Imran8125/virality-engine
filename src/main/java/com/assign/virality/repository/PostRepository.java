package com.assign.virality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.assign.virality.domain.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
}
