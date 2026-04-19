package com.assign.virality.dto;

public record CreatePostRequest(Long authorId, boolean isBot, String content) {}
