package com.assign.virality.controller;

import com.assign.virality.domain.Bot;
import com.assign.virality.domain.Comment;
import com.assign.virality.domain.Post;
import com.assign.virality.domain.User;
import com.assign.virality.dto.*;
import com.assign.virality.service.ViralityEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ViralityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ViralityEngineService viralityEngineService;

    @InjectMocks
    private ViralityController viralityController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(viralityController).build();
    }

    @Test
    void createUser_ShouldReturnCreatedUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setPremium(true);

        when(viralityEngineService.createUser(any(CreateUserRequest.class))).thenReturn(user);

        String json = "{\"username\":\"testUser\", \"isPremium\":true}";

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.premium").value(true));
    }

    @Test
    void createBot_ShouldReturnCreatedBot() throws Exception {
        Bot bot = new Bot();
        bot.setId(2L);
        bot.setName("botName");
        bot.setPersonaDescription("persona");

        when(viralityEngineService.createBot(any(CreateBotRequest.class))).thenReturn(bot);

        String json = "{\"name\":\"botName\", \"personaDescription\":\"persona\"}";

        mockMvc.perform(post("/api/bots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("botName"));
    }

    @Test
    void createPost_ShouldReturnCreatedPost() throws Exception {
        Post post = new Post();
        post.setId(10L);
        post.setAuthorId(1L);
        post.setContent("Hello World");

        when(viralityEngineService.createPost(any(CreatePostRequest.class))).thenReturn(post);

        String json = "{\"authorId\":1, \"isBot\":false, \"content\":\"Hello World\"}";

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.content").value("Hello World"));
    }

    @Test
    void addComment_ShouldReturnCreatedComment() throws Exception {
        Comment comment = new Comment();
        comment.setId(20L);
        comment.setPostId(10L);
        comment.setContent("Nice post");

        when(viralityEngineService.addComment(eq(10L), any(CreateCommentRequest.class))).thenReturn(comment);

        String json = "{\"authorId\":1, \"isBot\":false, \"content\":\"Nice post\"}";

        mockMvc.perform(post("/api/posts/10/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.content").value("Nice post"));
    }

    @Test
    void likePost_ShouldReturnOk() throws Exception {
        doNothing().when(viralityEngineService).likePost(eq(10L), any(LikeRequest.class));

        String json = "{\"authorId\":1, \"isBot\":false}";

        mockMvc.perform(post("/api/posts/10/like")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void getPostStats_ShouldReturnStats() throws Exception {
        PostStatsResponse response = new PostStatsResponse(10L, 500L);

        when(viralityEngineService.getPostStats(10L)).thenReturn(response);

        mockMvc.perform(get("/api/posts/10/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(10))
                .andExpect(jsonPath("$.viralityScore").value(500));
    }
}
