package com.assign.virality.dto;

public record CreateCommentRequest(Long authorId, boolean isBot, String content, Long parentCommentId) {}
