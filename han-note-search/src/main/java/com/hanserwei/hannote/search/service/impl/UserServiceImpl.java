package com.hanserwei.hannote.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;
import com.google.common.collect.Lists;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.utils.NumberUtils;
import com.hanserwei.hannote.search.index.UserIndex;
import com.hanserwei.hannote.search.model.vo.SearchUserReqVO;
import com.hanserwei.hannote.search.model.vo.SearchUserRspVO;
import com.hanserwei.hannote.search.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private ElasticsearchClient client;

    @Override
    public PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO) {
        // 查询关键词
        String keyword = searchUserReqVO.getKeyword();
        // 当前页码
        Integer pageNo = searchUserReqVO.getPageNo();

        int pageSize = 10; // 每页展示数据量
        int from = (pageNo - 1) * pageSize; // 偏移量

        HighlightField nicknameHighlight = HighlightField.of(hf -> hf
                .preTags("<strong>")
                .postTags("</strong>")
        );


        SearchRequest searchRequest = SearchRequest.of(r -> r
                .index(UserIndex.NAME)

                // 1. 构建 Query: multiMatchQuery (RHL 风格的匹配)
                .query(q -> q
                        .multiMatch(m -> m
                                .query(keyword)
                                .fields(UserIndex.FIELD_USER_NICKNAME, UserIndex.FIELD_USER_HAN_NOTE_ID)
                                // 默认使用 MatchQuery 行为，如果要模糊匹配，请添加 .fuzziness("AUTO")
                                .type(TextQueryType.PhrasePrefix)
                        )
                )

                // 2. 构建 Sort
                .sort(s -> s
                        .field(f -> f
                                .field(UserIndex.FIELD_USER_FANS_TOTAL)
                                .order(SortOrder.Desc)
                        )
                )

                .highlight(h -> h.fields(NamedValue.of(UserIndex.FIELD_USER_NICKNAME, nicknameHighlight)))

                // 3. 分页 from 和 size
                .from(from)
                .size(pageSize)
        );


        // 返参 VO 集合
        List<SearchUserRspVO> searchUserRspVOS = Lists.newArrayList();
        // 总文档数，默认为 0
        long total = 0;

        try {
            log.info("==> SearchRequest: {}", searchRequest.toString());

            // 执行查询请求
            SearchResponse<SearchUserRspVO> searchResponse = client.search(searchRequest, SearchUserRspVO.class);

            // 处理搜索结果
            List<Hit<SearchUserRspVO>> hits = searchResponse.hits().hits();
            if (searchResponse.hits().total() != null) {
                total = searchResponse.hits().total().value();
            }

            searchUserRspVOS = Lists.newArrayList();

            for (Hit<SearchUserRspVO> hit : hits) {
                // 1. 获取原始文档数据 (source)
                SearchUserRspVO source = hit.source();

                // 2. 获取高亮数据 (highlight)
                Map<String, List<String>> highlights = hit.highlight();

                if (source != null) {
                    // 3. 调用辅助方法合并数据和高亮
                    SearchUserRspVO searchUserRspVO = mergeHitToRspVO(source, highlights);
                    searchUserRspVOS.add(searchUserRspVO);
                }
            }

        } catch (Exception e) {
            log.error("==> 查询 Elasticsearch 异常: ", e);
        }

        return PageResponse.success(searchUserRspVOS, pageNo, total);
    }

    /**
     * 将原始文档和高亮数据合并到 SearchUserRspVO
     *
     * @param source     原始文档数据 (已自动反序列化)
     * @param highlights 高亮数据 Map
     * @return SearchUserRspVO
     */
    private SearchUserRspVO mergeHitToRspVO(SearchUserRspVO source, Map<String, List<String>> highlights) {
        if (source == null) {
            return null;
        }

        // 1. 复制原始文档字段 (假设 SearchUserRspVO 使用 Lombok @Data 或 Builder)
        SearchUserRspVO searchUserRspVO = SearchUserRspVO.builder()
                .userId(source.getUserId())
                .nickname(source.getNickname())
                .avatar(source.getAvatar())
                .hanNoteId(source.getHanNoteId()) // 字段名应与您的 VO 保持一致
                .noteTotal(source.getNoteTotal())
                .build();
        if (source.getFansTotal() != null) {
            searchUserRspVO.setFansTotal(NumberUtils.formatNumberString(Long.parseLong(source.getFansTotal())));
        }

        // 2. ⭐️ 核心逻辑：处理并设置高亮字段
        if (highlights != null) {
            // 尝试从 highlights Map 中获取 nickname 字段的高亮结果
            List<String> nicknameHighlights = highlights.get(UserIndex.FIELD_USER_NICKNAME);

            if (nicknameHighlights != null && !nicknameHighlights.isEmpty()) {
                searchUserRspVO.setHighlightNickname(nicknameHighlights.getFirst());
            }
        }

        // 3. 如果高亮字段为空，默认使用原始 nickname
        if (searchUserRspVO.getHighlightNickname() == null) {
            searchUserRspVO.setHighlightNickname(source.getNickname());
        }

        return searchUserRspVO;
    }
}
