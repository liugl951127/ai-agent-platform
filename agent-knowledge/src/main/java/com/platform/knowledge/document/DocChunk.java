package com.platform.knowledge.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.util.List;

@Data
@Document(indexName = "knowledge_chunk")
public class DocChunk {
    @Id
    private String id;
    @Field(type = FieldType.Long)
    private Long kbId;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;
    @Field(type = FieldType.Keyword)
    private List<String> tags;
}
