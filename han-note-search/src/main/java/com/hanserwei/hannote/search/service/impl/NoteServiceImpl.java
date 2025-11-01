package com.hanserwei.hannote.search.service.impl;

import cn.hutool.core.collection.CollUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.utils.NumberUtils;
import com.hanserwei.hannote.search.enums.NoteSortTypeEnum;
import com.hanserwei.hannote.search.index.NoteIndex;
import com.hanserwei.hannote.search.model.vo.SearchNoteReqVO;
import com.hanserwei.hannote.search.model.vo.SearchNoteRspVO;
import com.hanserwei.hannote.search.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        // 笔记类型
        Integer type = searchNoteReqVO.getType();
        // 排序方式
        Integer sort = searchNoteReqVO.getSort();

        // --- 2. 分页参数 ---
        int pageSize = 10;
        int from = (pageNo - 1) * pageSize;

        //条件查询
        // 创建查询条件
        //    "query": {
        //         "bool": {
        //           "must": [
        //             {
        //               "multi_match": {
        //                 "query": "壁纸",
        //                 "fields": [
        //                   "title^2.0",
        //                   "topic^1.0"
        //                 ]
        //               }
        //             }
        //           ],
        //           "filter": [
        //             {
        //               "term": {
        //                 "type": {
        //                   "value": 0
        //                 }
        //               }
        //             }
        //           ]
        //         }
        //       },
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(keyword)
                // 新客户端推荐在字段名中直接附加权重
                .fields(NoteIndex.FIELD_NOTE_TITLE + "^2.0", NoteIndex.FIELD_NOTE_TOPIC)
        );
        boolQueryBuilder.must(multiMatchQuery);
        // 3.2. 构建 term (filter)
        if (Objects.nonNull(type)) {
            boolQueryBuilder.filter(f -> f
                    .term(t -> t
                            .field(NoteIndex.FIELD_NOTE_TYPE)
                            .value(type) // .value() 会自动处理 Integer, Long, String 等
                    )
            );
        }
        BoolQuery boolQuery = boolQueryBuilder.build();
        // --- 4. 构建排序 (Sort) 和 FunctionScore ---
        Query finalQuery;
        List<SortOptions> sortOptions = CollUtil.newArrayList();
        NoteSortTypeEnum noteSortTypeEnum = NoteSortTypeEnum.valueOf(sort);
        if (Objects.nonNull(noteSortTypeEnum)) {
            // 4.1. CASE 1: 按字段排序
            finalQuery = boolQuery._toQuery(); // 查询主体就是 bool 查询

            switch (noteSortTypeEnum) {
                // 按笔记发布时间降序
                case LATEST -> sortOptions.add(SortOptions.of(s -> s
                        .field(f -> f.field(NoteIndex.FIELD_NOTE_CREATE_TIME).order(SortOrder.Desc))
                ));
                // 按笔记点赞量降序
                case MOST_LIKE -> sortOptions.add(SortOptions.of(s -> s
                        .field(f -> f.field(NoteIndex.FIELD_NOTE_LIKE_TOTAL).order(SortOrder.Desc))
                ));
                // 按评论量降序
                case MOST_COMMENT -> sortOptions.add(SortOptions.of(s -> s
                        .field(f -> f.field(NoteIndex.FIELD_NOTE_COMMENT_TOTAL).order(SortOrder.Desc))
                ));
                // 按收藏量降序
                case MOST_COLLECT -> sortOptions.add(SortOptions.of(s -> s
                        .field(f -> f.field(NoteIndex.FIELD_NOTE_COLLECT_TOTAL).order(SortOrder.Desc))
                ));
            }
        } else {
            // 4.2. CASE 2: 综合排序 (Function Score)
            // 综合排序，按 _score 降序
            sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("_score").order(SortOrder.Desc))));

            // 4.2.1. 构建 function_score 的 functions 列表
            List<FunctionScore> functions = new ArrayList<>();

            // Function 1: like_total
            functions.add(FunctionScore.of(fs -> fs
                    .fieldValueFactor(fvf -> fvf
                            .field(NoteIndex.FIELD_NOTE_LIKE_TOTAL)
                            .factor(0.5) // 新版客户端使用 double
                            .modifier(FieldValueFactorModifier.Sqrt)
                            .missing(0.0) // missing 值也为 double
                    )
            ));
            // 创建 FilterFunctionBuilder 数组
            // "functions": [
            //         {
            //           "field_value_factor": {
            //             "field": "like_total",
            //             "factor": 0.5,
            //             "modifier": "sqrt",
            //             "missing": 0
            //           }
            //         },
            //         {
            //           "field_value_factor": {
            //             "field": "collect_total",
            //             "factor": 0.3,
            //             "modifier": "sqrt",
            //             "missing": 0
            //           }
            //         },
            //         {
            //           "field_value_factor": {
            //             "field": "comment_total",
            //             "factor": 0.2,
            //             "modifier": "sqrt",
            //             "missing": 0
            //           }
            //         }
            //       ],
            // Function 2: collect_total
            functions.add(FunctionScore.of(fs -> fs
                    .fieldValueFactor(fvf -> fvf
                            .field(NoteIndex.FIELD_NOTE_COLLECT_TOTAL)
                            .factor(0.3)
                            .modifier(FieldValueFactorModifier.Sqrt)
                            .missing(0.0)
                    )
            ));

            // Function 3: comment_total
            functions.add(FunctionScore.of(fs -> fs
                    .fieldValueFactor(fvf -> fvf
                            .field(NoteIndex.FIELD_NOTE_COMMENT_TOTAL)
                            .factor(0.2)
                            .modifier(FieldValueFactorModifier.Sqrt)
                            .missing(0.0)
                    )
            ));

            // 4.2.2. 构建 FunctionScoreQuery
            FunctionScoreQuery functionScoreQuery = FunctionScoreQuery.of(fsq -> fsq
                    .query(boolQuery._toQuery()) // 基础查询
                    .functions(functions)        // 评分函数
                    .scoreMode(FunctionScoreMode.Sum) // 对应 score_mode
                    .boostMode(FunctionBoostMode.Sum) // 对应 boost_mode
            );

            finalQuery = functionScoreQuery._toQuery(); // 最终查询是 function_score
        }
        // --- 5. 构建高亮 (Highlight) ---
        HighlightField titleHighlight = HighlightField.of(hf -> hf
                .preTags("<strong>")
                .postTags("</strong>")
        );
        Highlight highlight = Highlight.of(h -> h.fields(NamedValue.of(NoteIndex.FIELD_NOTE_TITLE, titleHighlight)));
        // --- 6. 构建最终的 SearchRequest ---
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(NoteIndex.NAME)
                .query(finalQuery)   // 设置查询
                .sort(sortOptions) // 设置排序
                .from(from)        // 设置分页
                .size(pageSize)    // 设置分页
                .highlight(highlight) // 设置高亮
        );
        // --- 7. 执行查询和解析响应 ---
        List<SearchNoteRspVO> searchNoteRspVOS = new ArrayList<>();
        long total = 0;
        try {
            log.info("==> SearchRequest: {}", searchRequest.toString());

            // 执行查询请求
            SearchResponse<SearchNoteRspVO> searchResponse = client.search(searchRequest, SearchNoteRspVO.class);
            if (searchResponse.hits().total() != null) {
                total = searchResponse.hits().total().value();
            }
            log.info("==> 命中文档总数, hits: {}", total);
            List<Hit<SearchNoteRspVO>> hits = searchResponse.hits().hits();
            for (Hit<SearchNoteRspVO> hit : hits) {
                // 获取source
                SearchNoteRspVO source = hit.source();
                // 7.3. 获取高亮字段
                String highlightedTitle = null;
                Map<String, List<String>> highlightFields = hit.highlight();
                if (highlightFields.containsKey(NoteIndex.FIELD_NOTE_TITLE)) {
                    highlightedTitle = highlightFields.get(NoteIndex.FIELD_NOTE_TITLE).getFirst();
                }
                if (source != null) {
                    Long noteId = source.getNoteId();
                    String cover = source.getCover();
                    String title = source.getTitle();
                    String highlightTitle = source.getHighlightTitle();
                    String avatar = source.getAvatar();
                    String nickname = source.getNickname();
                    LocalDateTime updateTime = source.getUpdateTime();
                    String likeTotal = source.getLikeTotal();
                    searchNoteRspVOS.add(SearchNoteRspVO.builder()
                            .noteId(noteId)
                            .cover(cover)
                            .title(title)
                            .highlightTitle(highlightTitle)
                            .avatar(avatar)
                            .nickname(nickname)
                            .updateTime(updateTime)
                            .highlightTitle(highlightedTitle)
                            .likeTotal(NumberUtils.formatNumberString(Long.parseLong(likeTotal)))
                            .build());

                }
            }
        } catch (IOException e) {
            log.error("==> 搜索笔记异常: {}", e.getMessage());
        }
        return PageResponse.success(searchNoteRspVOS, pageNo, total);
    }

}
