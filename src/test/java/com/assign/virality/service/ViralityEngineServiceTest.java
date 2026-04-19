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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViralityEngineServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BotRepository botRepository;
    @Mock
    private RedisGuardrailService redisGuardrailService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ViralityEngineService viralityEngineService;

    @Test
    void createUser_ReturnsSavedUser() {
        CreateUserRequest request = new CreateUserRequest("testUser", true);
        User user = new User();
        user.setUsername("testUser");
        user.setPremium(true);

        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = viralityEngineService.createUser(request);

        assertEquals("testUser", result.getUsername());
        assertTrue(result.isPremium());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createBot_ReturnsSavedBot() {
        CreateBotRequest request = new CreateBotRequest("testBot", "persona");
        Bot bot = new Bot();
        bot.setName("testBot");
        bot.setPersonaDescription("persona");

        when(botRepository.save(any(Bot.class))).thenReturn(bot);

        Bot result = viralityEngineService.createBot(request);

        assertEquals("testBot", result.getName());
        assertEquals("persona", result.getPersonaDescription());
        verify(botRepository).save(any(Bot.class));
    }

    @Test
    void createPost_WhenHuman_ReturnsSavedPost() {
        CreatePostRequest request = new CreatePostRequest(1L, false, "human post");
        Post post = new Post();
        post.setAuthorId(1L);
        post.setContent("human post");

        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        Post result = viralityEngineService.createPost(request);

        assertEquals(1L, result.getAuthorId());
        assertEquals("human post", result.getContent());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void createPost_WhenBot_ReturnsSavedPost() {
        CreatePostRequest request = new CreatePostRequest(1L, true, "bot post");
        Post post = new Post();
        post.setAuthorId(1L);
        post.setContent("bot post");

        when(botRepository.findById(1L)).thenReturn(Optional.of(new Bot()));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        Post result = viralityEngineService.createPost(request);

        assertEquals(1L, result.getAuthorId());
        assertEquals("bot post", result.getContent());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void getPostStats_ReturnsStats() {
        when(postRepository.existsById(1L)).thenReturn(true);
        when(redisGuardrailService.getViralityScore(1L)).thenReturn(150L);

        PostStatsResponse response = viralityEngineService.getPostStats(1L);

        assertEquals(1L, response.postId());
        assertEquals(150L, response.viralityScore());
    }

    @Test
    void getPostStats_WhenPostNotFound_ThrowsException() {
        when(postRepository.existsById(1L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> viralityEngineService.getPostStats(1L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void likePost_WhenHuman_UpdatesScore() {
        LikeRequest request = new LikeRequest(2L, false);
        Post post = new Post();
        post.setAuthorId(1L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(new User()));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        viralityEngineService.likePost(1L, request);

        verify(redisGuardrailService).updateViralityScore(1L, 20);
        verify(redisGuardrailService, never()).enforceCooldownCap(anyLong(), anyLong());
    }

    @Test
    void likePost_WhenBotLikesHumanPost_EnforcesCooldownAndNotifies() {
        LikeRequest request = new LikeRequest(2L, true);
        Post post = new Post();
        post.setAuthorId(1L);

        when(botRepository.findById(2L)).thenReturn(Optional.of(new Bot()));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.existsById(1L)).thenReturn(true); // Is post by human

        viralityEngineService.likePost(1L, request);

        verify(redisGuardrailService).enforceCooldownCap(2L, 1L);
        verify(notificationService).notifyUser(1L, "Bot 2 liked your post");
        verify(redisGuardrailService, never()).updateViralityScore(anyLong(), anyInt());
    }
    
    @Test
    void addComment_HumanComment_UpdatesScore() {
        CreateCommentRequest request = new CreateCommentRequest(1L, false, "human comment", null);
        Post post = new Post();
        post.setAuthorId(2L);
        
        Comment comment = new Comment();
        comment.setPostId(1L);
        comment.setAuthorId(1L);
        comment.setDepthLevel(0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Comment result = viralityEngineService.addComment(1L, request);

        assertEquals(0, result.getDepthLevel());
        verify(redisGuardrailService).enforceVerticalCap(0);
        verify(redisGuardrailService).updateViralityScore(1L, 50);
        verify(commentRepository).save(any(Comment.class));
    }
    
    @Test
    void addComment_BotReplyToHuman_ChecksCapsAndNotifies() {
        CreateCommentRequest request = new CreateCommentRequest(2L, true, "bot reply", 10L);
        Post post = new Post();
        post.setAuthorId(1L);
        
        Comment parent = new Comment();
        parent.setAuthorId(1L);
        parent.setDepthLevel(0);
        
        Comment comment = new Comment();
        comment.setPostId(1L);
        comment.setAuthorId(2L);
        comment.setDepthLevel(1);

        when(botRepository.findById(2L)).thenReturn(Optional.of(new Bot()));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(userRepository.existsById(1L)).thenReturn(true); // Parent author is human
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Comment result = viralityEngineService.addComment(1L, request);

        assertEquals(1, result.getDepthLevel());
        verify(redisGuardrailService).enforceVerticalCap(1);
        verify(redisGuardrailService).enforceHorizontalCap(1L);
        verify(redisGuardrailService).enforceCooldownCap(2L, 1L);
        verify(notificationService).notifyUser(1L, "Bot 2 replied to your post");
        verify(redisGuardrailService).updateViralityScore(1L, 1);
    }

    @Test
    void createPost_WhenHumanNotFound_ThrowsException() {
        CreatePostRequest request = new CreatePostRequest(1L, false, "human post");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> viralityEngineService.createPost(request));
    }

    @Test
    void addComment_HumanReplyWithParent_UpdatesScore() {
        CreateCommentRequest request = new CreateCommentRequest(1L, false, "human comment", 10L);
        Comment parent = new Comment();
        parent.setDepthLevel(1);
        Post post = new Post();
        post.setAuthorId(2L);
        Comment newComment = new Comment();
        newComment.setDepthLevel(2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenReturn(newComment);
        
        Comment result = viralityEngineService.addComment(1L, request);
        
        assertEquals(2, result.getDepthLevel());
        verify(redisGuardrailService).enforceVerticalCap(2);
        verify(redisGuardrailService).updateViralityScore(1L, 50);
    }

    @Test
    void likePost_WhenBotLikesBotPost_DoesNotNotifyOrUpdateScore() {
        LikeRequest request = new LikeRequest(2L, true);
        Post post = new Post();
        post.setAuthorId(1L);

        when(botRepository.findById(2L)).thenReturn(Optional.of(new Bot()));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.existsById(1L)).thenReturn(false);

        viralityEngineService.likePost(1L, request);

        verify(redisGuardrailService, never()).enforceCooldownCap(anyLong(), anyLong());
        verify(notificationService, never()).notifyUser(anyLong(), anyString());
        verify(redisGuardrailService, never()).updateViralityScore(anyLong(), anyInt());
    }
}
