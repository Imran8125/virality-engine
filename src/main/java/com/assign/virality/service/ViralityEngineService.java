package com.assign.virality.service;

import com.assign.virality.domain.Bot;
import com.assign.virality.domain.Comment;
import com.assign.virality.domain.Post;
import com.assign.virality.domain.User;
import com.assign.virality.dto.*;
import com.assign.virality.repository.BotRepository;
import com.assign.virality.repository.CommentRepository;
import com.assign.virality.repository.PostRepository;
import com.assign.virality.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ViralityEngineService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final RedisGuardrailService redisGuardrailService;
    private final NotificationService notificationService;

    public ViralityEngineService(PostRepository postRepository,
                                 CommentRepository commentRepository,
                                 UserRepository userRepository,
                                 BotRepository botRepository,
                                 RedisGuardrailService redisGuardrailService,
                                 NotificationService notificationService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.botRepository = botRepository;
        this.redisGuardrailService = redisGuardrailService;
        this.notificationService = notificationService;
    }
    
    public User createUser(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPremium(request.isPremium());
        return userRepository.save(user);
    }
    
    public Bot createBot(CreateBotRequest request) {
        Bot bot = new Bot();
        bot.setName(request.name());
        bot.setPersonaDescription(request.personaDescription());
        return botRepository.save(bot);
    }

    @Transactional
    public Post createPost(CreatePostRequest request) {
        validateAuthor(request.authorId(), request.isBot());
        Post post = new Post();
        post.setAuthorId(request.authorId());
        post.setContent(request.content());
        return postRepository.save(post);
    }

    public PostStatsResponse getPostStats(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        Long score = redisGuardrailService.getViralityScore(postId);
        return new PostStatsResponse(postId, score);
    }

    @Transactional
    public void likePost(Long postId, LikeRequest request) {
        validateAuthor(request.authorId(), request.isBot());
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        
        // Cooldown Check if Bot likes human's post
        if (request.isBot()) {
            boolean isPostByHuman = isAuthorHuman(post.getAuthorId());
            if (isPostByHuman) {
                redisGuardrailService.enforceCooldownCap(request.authorId(), post.getAuthorId());
                notificationService.notifyUser(post.getAuthorId(), "Bot " + request.authorId() + " liked your post");
            }
        }

        // Only human likes give 20 points
        if (!request.isBot()) {
            redisGuardrailService.updateViralityScore(postId, 20);
        }
    }

    @Transactional
    public Comment addComment(Long postId, CreateCommentRequest request) {
        validateAuthor(request.authorId(), request.isBot());
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        int depth = 0;
        if (request.parentCommentId() != null) {
            Comment parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));
            depth = parent.getDepthLevel() + 1;
        }

        // Vertical Cap Check
        redisGuardrailService.enforceVerticalCap(depth);

        // Author specific checks
        if (request.isBot()) {
            // Horizontal Cap
            redisGuardrailService.enforceHorizontalCap(postId);

            boolean interactsWithHuman = false;
            Long targetHumanId = null;

            if (request.parentCommentId() != null) {
                Comment parent = commentRepository.findById(request.parentCommentId()).get();
                if (isAuthorHuman(parent.getAuthorId())) {
                    interactsWithHuman = true;
                    targetHumanId = parent.getAuthorId();
                }
            } else {
                if (isAuthorHuman(post.getAuthorId())) {
                    interactsWithHuman = true;
                    targetHumanId = post.getAuthorId();
                }
            }

            if (interactsWithHuman) {
                // Cooldown Cap
                redisGuardrailService.enforceCooldownCap(request.authorId(), targetHumanId);
                // Notifications Check
                notificationService.notifyUser(targetHumanId, "Bot " + request.authorId() + " replied to your post");
            }
            // Bot Reply = +1 Point
            redisGuardrailService.updateViralityScore(postId, 1);
        } else {
            // Human Comment = +50 Points
            redisGuardrailService.updateViralityScore(postId, 50);
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(request.authorId());
        comment.setContent(request.content());
        comment.setDepthLevel(depth);

        return commentRepository.save(comment);
    }
    
    private void validateAuthor(Long authorId, boolean isBot) {
        if (isBot) {
            botRepository.findById(authorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bot not found"));
        } else {
            userRepository.findById(authorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        }
    }
    
    private boolean isAuthorHuman(Long authorId) {
        // Technically author_id on Post or Comment could be bot or human. We would check if it exists in User, 
        // but since IDs could overlap if we are not careful.
        // Assuming if it is found in UserRepository, it is considered Human for this proof-of-concept.
        return userRepository.existsById(authorId);
    }
}
