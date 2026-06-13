package com.platform.llm.controller;

import com.platform.common.core.R;
import com.platform.llm.entity.LlmModel;
import com.platform.llm.mapper.LlmModelMapper;
import com.platform.llm.service.LlmRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmModelMapper modelMapper;
    private final LlmRouter llm;

    @GetMapping("/list")
    public R<List<LlmModel>> list() {
        return R.ok(modelMapper.selectList(null));
    }

    @PostMapping("/add")
    public R<?> add(@RequestBody LlmModel m) { return R.ok(modelMapper.insert(m)); }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam Long modelId,
                               @RequestBody List<Map<String,Object>> messages) {
        return llm.streamById(modelId, messages);
    }

    @PostMapping("/chat")
    public R<String> chat(@RequestParam Long modelId,
                          @RequestBody List<Map<String,Object>> messages) {
        return R.ok(llm.chatById(modelId, messages));
    }
}
