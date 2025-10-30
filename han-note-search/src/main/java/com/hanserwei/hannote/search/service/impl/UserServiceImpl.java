package com.hanserwei.hannote.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.google.common.collect.Lists;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.hannote.search.index.UserIndex;
import com.hanserwei.hannote.search.model.vo.SearchUserReqVO;
import com.hanserwei.hannote.search.model.vo.SearchUserRspVO;
import com.hanserwei.hannote.search.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private ElasticsearchClient client;

    /**
     * 获取 SearchUserRspVO
     *
     * @param hit 搜索结果
     * @return SearchUserRspVO
     */
    private static SearchUserRspVO getSearchUserRspVO(Hit<SearchUserRspVO> hit) {
        SearchUserRspVO searchUserRspVO = new SearchUserRspVO();

        SearchUserRspVO source = hit.source();
        if (source != null) {
            searchUserRspVO.setUserId(source.getUserId());
            searchUserRspVO.setNickname(source.getNickname());
            searchUserRspVO.setAvatar(source.getAvatar());
            searchUserRspVO.setHanNoteId(source.getHanNoteId());
            searchUserRspVO.setNoteTotal(source.getNoteTotal());
            searchUserRspVO.setFansTotal(source.getFansTotal());
        }
        return searchUserRspVO;
    }

    @Override
    public PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO) {
        // 查询关键字
        String keyword = searchUserReqVO.getKeyword();
        // 当前页码
        Integer pageNo = searchUserReqVO.getPageNo();

        int pageSize = 10;

        // 构建SearchRequest，指定索引
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(UserIndex.NAME)
                .query(query -> query
                        .multiMatch(multiMatch -> multiMatch
                                .query(keyword)
                                .fields(UserIndex.FIELD_USER_NICKNAME, UserIndex.FIELD_USER_HAN_NOTE_ID)
                                .type(TextQueryType.PhrasePrefix)))
                .sort(sort -> sort
                        .field(filedSort -> filedSort.field(UserIndex.FIELD_USER_FANS_TOTAL).order(SortOrder.Desc)))
                .from((pageNo - 1) * pageSize)
                .size(pageSize)
                .build();

        // 返参 VO 集合
        List<SearchUserRspVO> searchUserRspVOS = null;
        // 总文档数，默认为 0
        long total = 0;
        try {
            log.info("==> SearchRequest:{}", searchRequest);

            // 执行查询请求
            SearchResponse<SearchUserRspVO> searchResponse = client.search(searchRequest, SearchUserRspVO.class);

            searchUserRspVOS = Lists.newArrayList();

            // 处理搜索结果
            List<Hit<SearchUserRspVO>> hits = searchResponse.hits().hits();
            if (searchResponse.hits().total() != null) {
                total = searchResponse.hits().total().value();
            }

            for (Hit<SearchUserRspVO> hit : hits) {
                log.info("==> 文档数据: {}", hit.toString());
                if (hit.source() != null) {
                    SearchUserRspVO searchUserRspVO = getSearchUserRspVO(hit);
                    searchUserRspVOS.add(searchUserRspVO);
                }
            }


        } catch (Exception e) {
            log.error("==> 查询 Elasticsearch 异常: ", e);
        }
        return PageResponse.success(searchUserRspVOS, pageNo, total);
    }
}
