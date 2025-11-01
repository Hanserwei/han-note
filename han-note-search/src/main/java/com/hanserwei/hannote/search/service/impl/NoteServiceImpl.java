package com.hanserwei.hannote.search.service.impl;

import cn.hutool.core.collection.CollUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.utils.NumberUtils;
import com.hanserwei.hannote.search.index.NoteIndex;
import com.hanserwei.hannote.search.model.vo.SearchNoteReqVO;
import com.hanserwei.hannote.search.model.vo.SearchNoteRspVO;
import com.hanserwei.hannote.search.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {

    @Resource
    private ElasticsearchClient client;

    @Override
    public PageResponse<SearchNoteRspVO> searchNote(SearchNoteReqVO searchNoteReqVO) {
        // 查询关键词
        String keyword = searchNoteReqVO.getKeyword();
        // 当前页码
        Integer pageNo = searchNoteReqVO.getPageNo();

        int pageSize = 10; // 每页展示数据量
        int from = (pageNo - 1) * pageSize; // 偏移量

        // 1. 构建基础 Multi-Match Query (原始查询条件)
        MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(keyword)
                // 字段权重设置，与 RHL 的 .field(field, boost) 对应
                .fields(NoteIndex.FIELD_NOTE_TITLE + "^2.0", NoteIndex.FIELD_NOTE_TOPIC)
        );
        Query functionScoreQuery = Query.of(q -> q
                .functionScore(fs -> fs
                        // 设置初始查询条件
                        .query(innerQuery -> innerQuery.multiMatch(multiMatchQuery))
                        .scoreMode(FunctionScoreMode.Sum)
                        .boostMode(FunctionBoostMode.Sum)
                        // 设置function数组
                        .functions(List.of(
                                // 评分函数1
                                FunctionScore.of(f -> f.fieldValueFactor(fvf -> fvf
                                        .field(NoteIndex.FIELD_NOTE_LIKE_TOTAL)
                                        .factor(0.5)
                                        .modifier(FieldValueFactorModifier.Sqrt)
                                        .missing(0.0))),
                                // 评分函数2
                                FunctionScore.of(f -> f.fieldValueFactor(fvf -> fvf
                                        .field(NoteIndex.FIELD_NOTE_COLLECT_TOTAL)
                                        .factor(0.3)
                                        .modifier(FieldValueFactorModifier.Sqrt)
                                        .missing(0.0))),
                                // 评分函数3
                                FunctionScore.of(f -> f.fieldValueFactor(fvf -> fvf
                                        .field(NoteIndex.FIELD_NOTE_COMMENT_TOTAL)
                                        .factor(0.2)
                                        .modifier(FieldValueFactorModifier.Sqrt)
                                        .missing(0.0)))
                        ))
                )
        );
        // 3. 构建 Highlight 配置
        HighlightField titleHighlight = HighlightField.of(hf -> hf
                .preTags("<strong>")
                .postTags("</strong>")
        );
        // 4. 构建最终的 SearchRequest
        SearchRequest searchRequest = SearchRequest.of(r -> r
                .index(NoteIndex.NAME)
                .query(functionScoreQuery) // 设置 function_score 查询

                // 排序：按 _score 降序
                .sort(s -> s.score(d -> d.order(SortOrder.Desc)))

                // 分页
                .from(from)
                .size(pageSize)
                // 高亮
                .highlight(h -> h
                        .fields(NamedValue.of(NoteIndex.FIELD_NOTE_TITLE, titleHighlight))
                )
        );
        // 返参 VO 集合
        List<SearchNoteRspVO> searchNoteRspVOS = null;
        long total = 0;

        try {
            log.info("==> NoteSearchRequest: {}", searchRequest.toString());

            // ⭐️ 执行查询请求，并自动反序列化文档源到 SearchNoteRspVO
            SearchResponse<SearchNoteRspVO> searchResponse =
                    client.search(searchRequest, SearchNoteRspVO.class);

            total = searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0;
            log.info("==> 命中文档总数, hits: {}", total);

            // ⭐️ 处理搜索结果：合并原始文档和高亮数据
            searchNoteRspVOS = searchResponse.hits().hits().stream()
                    .map(this::processNoteHit)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("==> 查询 Elasticsearch 异常: ", e);
        }

        return PageResponse.success(searchNoteRspVOS, pageNo, total);
    }

    /**
     * 辅助方法：处理 Hit，合并 Source 和 Highlight 数据
     */
    private SearchNoteRspVO processNoteHit(Hit<SearchNoteRspVO> hit) {
        SearchNoteRspVO rspVO = hit.source();

        if (rspVO == null) {
            return null;
        }

        // 2. ⭐️ 处理高亮字段
        Map<String, List<String>> highlights = hit.highlight();

        if (CollUtil.isNotEmpty(highlights)) {
            List<String> titleHighlights = highlights.get(NoteIndex.FIELD_NOTE_TITLE);
            if (CollUtil.isNotEmpty(titleHighlights)) {
                // 设置高亮标题
                rspVO.setHighlightTitle(titleHighlights.getFirst());
            }
        }
        // 3. 确保 highlightTitle 有值 (如果没有高亮结果，使用原始 title)
        if (rspVO.getHighlightTitle() == null) {
            rspVO.setHighlightTitle(rspVO.getTitle());
        }
        // 4. 处理特殊格式化（如 RHL 代码中的点赞数格式化）
        if (rspVO.getLikeTotal() != null) {
            rspVO.setLikeTotal(NumberUtils.formatNumberString(Long.parseLong(rspVO.getLikeTotal())));
        }
        return rspVO;
    }
}
