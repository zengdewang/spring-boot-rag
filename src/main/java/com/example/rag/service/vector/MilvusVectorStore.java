package com.example.rag.service.vector;

import com.example.rag.common.exception.BusinessException;
import com.example.rag.config.EmbeddingProperties;
import com.example.rag.config.MilvusProperties;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import io.milvus.v2.service.vector.response.SearchResp.SearchResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Milvus 向量库封装：集合管理、向量插入、相似度检索、按文档删除。
 * 集中隔离 milvus-sdk-java 的 API 细节，业务层只依赖本类。
 * 注意：更换 Embedding 模型（维度变化）时需删除集合重建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorStore {

    private final MilvusClientV2 client;
    private final MilvusProperties props;
    private final EmbeddingProperties embeddingProps;

    @PostConstruct
    public void init() {
        ensureCollection();
    }

    private void ensureCollection() {
        String collection = props.getCollection();
        if (client.hasCollection(HasCollectionReq.builder().collectionName(collection).build())) {
            log.info("Milvus 集合已存在: {}", collection);
            return;
        }
        CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder().build();
        schema.addField(AddFieldReq.builder().fieldName("id").dataType(DataType.Int64).isPrimaryKey(true).autoID(true).build());
        schema.addField(AddFieldReq.builder().fieldName("doc_id").dataType(DataType.Int64).build());
        schema.addField(AddFieldReq.builder().fieldName("chunk_index").dataType(DataType.Int32).build());
        schema.addField(AddFieldReq.builder().fieldName("text").dataType(DataType.VarChar).maxLength(4096).build());
        schema.addField(AddFieldReq.builder().fieldName("vector").dataType(DataType.FloatVector).dimension(embeddingProps.getDimension()).build());

        IndexParam index = IndexParam.builder()
                .fieldName("vector")
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .metricType(IndexParam.MetricType.COSINE)
                .build();

        client.createCollection(CreateCollectionReq.builder()
                .collectionName(collection)
                .collectionSchema(schema)
                .indexParams(List.of(index))
                .build());
        log.info("Milvus 集合已创建: {} (维度 {})", collection, embeddingProps.getDimension());
    }

    /**
     * 批量插入分块向量。
     *
     * @param docId   文档 ID
     * @param chunks  分块文本（顺序与 vectors 一一对应）
     * @param vectors 各分块向量
     */
    public void insert(Long docId, List<String> chunks, List<List<Float>> vectors) {
        if (chunks == null || chunks.isEmpty() || chunks.size() != vectors.size()) {
            throw new BusinessException(500, "分块与向量数量不一致");
        }
        List<JsonObject> rows = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            JsonObject row = new JsonObject();
            row.addProperty("doc_id", docId);
            row.addProperty("chunk_index", i);
            row.addProperty("text", chunks.get(i));

            JsonArray vec = new JsonArray();
            for (Float f : vectors.get(i)) {
                vec.add(f);
            }
            row.add("vector", vec);
            rows.add(row);
        }
        client.insert(InsertReq.builder()
                .collectionName(props.getCollection())
                .data(rows)
                .build());
        log.info("插入 Milvus 成功: docId={}, chunkCount={}", docId, rows.size());
    }

    /**
     * 向量相似度检索，返回 TopK 结果（含 doc_id / chunk_index / text / score）。
     */
    public List<SearchResult> search(List<Float> queryVector, int topK) {
        List<BaseVector> queryVectors = List.of(new FloatVec(queryVector));
        SearchResp resp = client.search(SearchReq.builder()
                .collectionName(props.getCollection())
                .data(queryVectors)
                .topK(topK)
                .outputFields(List.of("doc_id", "chunk_index", "text"))
                .build());
        List<List<SearchResult>> results = resp.getSearchResults();
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.get(0);
    }

    /** 按文档 ID 删除其全部向量（删除文档时调用） */
    public void deleteByDocId(Long docId) {
        client.delete(DeleteReq.builder()
                .collectionName(props.getCollection())
                .filter("doc_id == " + docId)
                .build());
        log.info("删除 Milvus 向量: docId={}", docId);
    }
}
