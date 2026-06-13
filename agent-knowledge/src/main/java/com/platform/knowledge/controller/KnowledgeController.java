package com.platform.knowledge.controller;

import com.platform.common.core.R;
import com.platform.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeService service;

    @PostMapping("/index")
    public R<?> index(@RequestParam Long kbId,
                      @RequestParam String content,
                      @RequestParam(required = false) List<String> tags) {
        service.index(kbId, content, tags);
        return R.ok();
    }

    @PostMapping("/search")
    public R<List<String>> search(@RequestParam Long kbId,
                                  @RequestParam String q,
                                  @RequestParam(defaultValue = "3") int topK) {
        try {
            return R.ok(service.search(kbId, q, topK));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
