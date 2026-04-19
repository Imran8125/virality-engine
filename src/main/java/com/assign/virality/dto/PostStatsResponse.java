package com.assign.virality.dto;

public record PostStatsResponse(
        Long postId,
        Long viralityScore
) {}
