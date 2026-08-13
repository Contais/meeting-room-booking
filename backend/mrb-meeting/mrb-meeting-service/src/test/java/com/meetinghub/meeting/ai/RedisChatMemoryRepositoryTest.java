package com.meetinghub.meeting.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisChatMemoryRepositoryTest {

    private static final String CONVERSATION_ID = "abc123";
    private static final String REDIS_KEY = "mrb:chat:memory:abc123";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final AtomicReference<String> store = new AtomicReference<>();

    private RedisChatMemoryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RedisChatMemoryRepository(redisTemplate, new ObjectMapper());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void should_roundTripMessages_when_saveAndFind() {
        mockReadWrite();
        List<Message> messages = List.of(
                new UserMessage("帮我约明天的会议室"),
                new AssistantMessage("好的，请问几点？")
        );

        repository.saveAll(CONVERSATION_ID, messages);
        List<Message> result = repository.findByConversationId(CONVERSATION_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isInstanceOf(UserMessage.class);
        assertThat(result.get(0).getText()).isEqualTo("帮我约明天的会议室");
        assertThat(result.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(result.get(1).getText()).isEqualTo("好的，请问几点？");
    }

    @Test
    void should_setTtl_when_save() {
        doAnswer(invocation -> {
            store.set(invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        repository.saveAll(CONVERSATION_ID, List.of(new UserMessage("hi")));

        verify(valueOperations).set(eq(REDIS_KEY), anyString(), eq(Duration.ofDays(7)));
    }

    @Test
    void should_persistToolMessageAsEmpty_when_save() {
        mockReadWrite();

        repository.saveAll(CONVERSATION_ID,
                List.of(ToolResponseMessage.builder().responses(List.of()).build()));
        List<Message> result = repository.findByConversationId(CONVERSATION_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessageType()).isEqualTo(MessageType.TOOL);
        assertThat(result.get(0).getText()).isEmpty();
    }

    @Test
    void should_preserveSystemMessage_when_save() {
        mockReadWrite();

        repository.saveAll(CONVERSATION_ID, List.of(new SystemMessage("system rule")));
        List<Message> result = repository.findByConversationId(CONVERSATION_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(result.get(0).getText()).isEqualTo("system rule");
    }

    @Test
    void should_returnEmpty_when_conversationMissing() {
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);

        List<Message> result = repository.findByConversationId(CONVERSATION_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_jsonMalformed() {
        when(valueOperations.get(REDIS_KEY)).thenReturn("{not-json");

        List<Message> result = repository.findByConversationId(CONVERSATION_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_redisReadFails() {
        when(valueOperations.get(REDIS_KEY))
                .thenThrow(new RuntimeException("redis unavailable"));

        List<Message> result = repository.findByConversationId(CONVERSATION_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void should_notThrow_when_redisWriteFails() {
        doThrow(new RuntimeException("redis unavailable"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        repository.saveAll(CONVERSATION_ID, List.of(new UserMessage("hi")));
    }

    @Test
    void should_deleteConversation_when_deleteByConversationId() {
        when(redisTemplate.delete(REDIS_KEY)).thenReturn(true);

        repository.deleteByConversationId(CONVERSATION_ID);

        verify(redisTemplate).delete(REDIS_KEY);
    }

    @Test
    void should_returnConversationIds_when_keysExist() {
        when(redisTemplate.keys("mrb:chat:memory:*"))
                .thenReturn(Set.of("mrb:chat:memory:abc123", "mrb:chat:memory:def456"));

        List<String> ids = repository.findConversationIds();

        assertThat(ids).containsExactlyInAnyOrder("abc123", "def456");
    }

    private void mockReadWrite() {
        when(valueOperations.get(REDIS_KEY)).thenAnswer(invocation -> store.get());
        doAnswer(invocation -> {
            store.set(invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }
}
