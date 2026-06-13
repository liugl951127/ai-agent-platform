package com.platform.knowledge.service;

import com.platform.knowledge.document.DocChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeService {
    private final ElasticsearchOperations es;

    public void index(Long kbId, String content, List<String> tags) {
        DocChunk c = new DocChunk();
        c.setKbId(kbId);
        c.setContent(content);
        c.setTags(tags);
        es.save(c);
    }

    public List<String> search(Long kbId, String q, int topK) {
        NativeQuery nq = NativeQuery.builder()
            .withQuery(qb -> qb.bool(b -> b
                .must(m -> m.match(mq -> mq.field("content").query(q)))
                .filter(f -> f.term(t -> t.field("kbId").value(kbId)))))
            .withMaxResults(topK)
            .build();
        SearchHits<DocChunk> hits = es.search(nq, DocChunk.class);
        List<String> out = new ArrayList<>();
        hits.forEach(h -> out.add(h.getContent()));
        return out;
    }
}
