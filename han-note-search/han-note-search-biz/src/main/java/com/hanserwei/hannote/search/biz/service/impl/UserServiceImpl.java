package com.hanserwei.hannote.search.biz.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.utils.NumberUtils;
import com.hanserwei.hannote.search.biz.domain.mapper.SelectMapper;
import com.hanserwei.hannote.search.biz.index.UserIndex;
import com.hanserwei.hannote.search.biz.model.vo.SearchUserReqVO;
import com.hanserwei.hannote.search.biz.model.vo.SearchUserRspVO;
import com.hanserwei.hannote.search.biz.service.UserService;
import com.hanserwei.hannote.search.dto.RebuildUserDocumentReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private ElasticsearchClient client;
    @Resource
    private SelectMapper selectMapper;

    @Override
    public PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO) {
        // --- 2. 获取请求参数 ---
        String keyword = searchUserReqVO.getKeyword();
        Integer pageNo = searchUserReqVO.getPageNo();

        // --- 3. 设置分页 ---
        int pageSize = 10; // 假设每页大小为10
        int from = (pageNo - 1) * pageSize;
        // --- 4. 构建 multi_match 查询 ---
        Query multiMatchQuery = Query.of(q -> q
                .multiMatch(m -> m
                        .query(keyword)
                        .type(TextQueryType.PhrasePrefix)
                        .fields(UserIndex.FIELD_USER_NICKNAME, UserIndex.FIELD_USER_HAN_NOTE_ID)
                )
        );
        // --- 5. 构建排序 ---
        SortOptions sortOptions = SortOptions.of(so -> so
                .field(f -> f
                        .field(UserIndex.FIELD_USER_FANS_TOTAL)
                        .order(SortOrder.Desc)));
        // --- 6. 构建高亮 ---
        HighlightField nikeNameHighlight = HighlightField.of(hf -> hf
                .preTags("<strong>")
                .postTags("</strong>")
        );
        Highlight highlight = Highlight.of(h -> h
                .fields(NamedValue.of(UserIndex.FIELD_USER_NICKNAME, nikeNameHighlight)));
        // --- 7. 构建 SearchRequest ---
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(UserIndex.NAME)
                .query(multiMatchQuery)
                .sort(sortOptions)
                .highlight(highlight)
                .from(from)
                .size(pageSize));
        // --- 8. 执行查询和解析响应 ---
        List<SearchUserRspVO> searchUserRspVOS = new ArrayList<>();
        long total = 0;
        try {
            log.info("==> searchRequest: {}", searchRequest);
            SearchResponse<SearchUserRspVO> searchResponse = client.search(searchRequest, SearchUserRspVO.class);
            // 8.2. 处理响应
            if (searchResponse.hits().total() != null) {
                total = searchResponse.hits().total().value();
            }
            log.info("==> 命中文档总数, hits: {}", total);
            List<Hit<SearchUserRspVO>> hits = searchResponse.hits().hits();
            for (Hit<SearchUserRspVO> hit : hits) {
                // 获取source
                SearchUserRspVO source = hit.source();
                // 8.3. 获取高亮字段
                String highlightNickname = null;
                Map<String, List<String>> highlightFiled = hit.highlight();
                if (highlightFiled.containsKey(UserIndex.FIELD_USER_NICKNAME)) {
                    highlightNickname = highlightFiled.get(UserIndex.FIELD_USER_NICKNAME).getFirst();
                }
                if (source != null) {
                    Long userId = source.getUserId();
                    String nickname = source.getNickname();
                    String avatar = source.getAvatar();
                    String hanNoteId = source.getHanNoteId();
                    Integer noteTotal = source.getNoteTotal();
                    String fansTotal = source.getFansTotal();
                    searchUserRspVOS.add(SearchUserRspVO.builder()
                            .userId(userId)
                            .nickname(nickname)
                            .highlightNickname(highlightNickname)
                            .avatar(avatar)
                            .hanNoteId(hanNoteId)
                            .noteTotal(noteTotal)
                            .fansTotal(fansTotal == null ? "0" : NumberUtils.formatNumberString(Long.parseLong(fansTotal)))
                            .build());
                }
            }
        } catch (IOException e) {
            log.error("==> search error: {}", e.getMessage());
        }
        return PageResponse.success(searchUserRspVOS, pageNo, total);
    }

    @Override
    public Response<Long> rebuildDocument(RebuildUserDocumentReqDTO rebuildUserDocumentReqDTO) {
        Long userId = rebuildUserDocumentReqDTO.getId();

        // 从数据库查询 Elasticsearch 索引数据
        List<Map<String, Object>> result = selectMapper.selectEsUserIndexData(userId);

        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : result) {
            IndexRequest<Object> request = IndexRequest.of(indexRequest -> indexRequest
                    // 创建索引请求对象，指定索引名称
                    .index(UserIndex.NAME)
                    // 设置文档的 ID，使用记录中的主键 “id” 字段值
                    .id((String.valueOf(recordMap.get(UserIndex.FIELD_USER_ID))))
                    // 设置文档的内容，使用查询结果的记录数据
                    .document(recordMap));
            // 将数据写入 Elasticsearch 索引
            try {
                client.index(request);
            } catch (IOException e) {
                log.error("==> 同步用户数据到 Elasticsearch 索引中失败: " + e.getMessage());
            }
        }
        return Response.success();
    }
}
