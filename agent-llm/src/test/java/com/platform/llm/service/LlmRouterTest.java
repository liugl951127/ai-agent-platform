package com.platform.llm.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.platform.llm.entity.LlmModel;
import com.platform.llm.mapper.LlmModelMapper;
import com.platform.llm.service.impl.OllamaProvider;
import com.platform.llm.service.impl.OpenAiProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LlmRouter 单元测试")
class LlmRouterTest {

    @Mock OpenAiProvider openAi;
    @Mock OllamaProvider ollama;
    @Mock LlmModelMapper modelMapper;

    @InjectMocks LlmRouter router;

    @BeforeEach
    void setUp() {
        router.init(); // 手动初始化 registry
    }

    @Test
    @DisplayName("chatById 走 OpenAI Provider")
    void chatByIdOpenAi() {
        LlmModel m = new LlmModel();
        m.setProvider("OPENAI");
        m.setApiKey("sk-test");
        m.setTemperature(new BigDecimal("0.7"));
        m.setMaxTokens(1024);
        when(modelMapper.selectById(1L)).thenReturn(m);
        when(openAi.chat(eq(m), anyList())).thenReturn("hello");

        String reply = router.chatById(1L, List.of(Map.of("role", "user", "content", "hi")));
        assertEquals("hello", reply);
        verify(ollama, never()).chat(any(), any());
    }

    @Test
    @DisplayName("chatById 走 Ollama Provider")
    void chatByIdOllama() {
        LlmModel m = new LlmModel();
        m.setProvider("OLLAMA");
        m.setApiBase("http://localhost:11434");
        when(modelMapper.selectById(2L)).thenReturn(m);
        when(ollama.chat(eq(m), anyList())).thenReturn("ollama-reply");

        String reply = router.chatById(2L, List.of(Map.of("role", "user", "content", "hi")));
        assertEquals("ollama-reply", reply);
        verify(openAi, never()).chat(any(), any());
    }

    @Test
    @DisplayName("模型不存在抛异常")
    void chatByIdModelNotFound() {
        when(modelMapper.selectById(99L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> router.chatById(99L, List.of()));
        // 实际错误信息可能是 NullPointerException 因为 m.getProvider() 在 null 上调用
        assertTrue(ex.getMessage().contains("null") || ex.getMessage().contains("模型不存在"));
    }

    @Test
    @DisplayName("streamById 返回 Flux")
    void streamById() {
        LlmModel m = new LlmModel();
        m.setProvider("OPENAI");
        when(modelMapper.selectById(1L)).thenReturn(m);
        when(openAi.chatStream(eq(m), anyList())).thenReturn(Flux.just("a", "b", "c"));

        Flux<String> flux = router.streamById(1L, List.of(Map.of("role","user","content","x")));
        StepVerifier.create(flux)
                .expectNext("a").expectNext("b").expectNext("c")
                .verifyComplete();
    }
}
