package com.assign.virality.controller;

import com.assign.virality.domain.Bot;
import com.assign.virality.domain.Comment;
import com.assign.virality.domain.Post;
import com.assign.virality.domain.User;
import com.assign.virality.dto.*;
import com.assign.virality.service.ViralityEngineService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ViralityController {

    private final ViralityEngineService viralityEngineService;

    public ViralityController(ViralityEngineService viralityEngineService) {
        this.viralityEngineService = viralityEngineService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody CreateUserRequest request) {
        return viralityEngineService.createUser(request);
    }

    @PostMapping("/bots")
    @ResponseStatus(HttpStatus.CREATED)
    public Bot createBot(@RequestBody CreateBotRequest request) {
        return viralityEngineService.createBot(request);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public Post createPost(@RequestBody CreatePostRequest request) {
        return viralityEngineService.createPost(request);
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Comment addComment(@PathVariable Long postId, @RequestBody CreateCommentRequest request) {
        return viralityEngineService.addComment(postId, request);
    }

    @PostMapping("/posts/{postId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void likePost(@PathVariable Long postId, @RequestBody LikeRequest request) {
        viralityEngineService.likePost(postId, request);
    }

    @GetMapping("/posts/{postId}/stats")
    public PostStatsResponse getPostStats(@PathVariable Long postId) {
        return viralityEngineService.getPostStats(postId);
    }
}
