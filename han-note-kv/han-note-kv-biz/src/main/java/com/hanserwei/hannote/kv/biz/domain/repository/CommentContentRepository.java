package com.hanserwei.hannote.kv.biz.domain.repository;

import com.hanserwei.hannote.kv.biz.domain.dataobject.CommentContentDO;
import com.hanserwei.hannote.kv.biz.domain.dataobject.CommentContentPrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public interface CommentContentRepository extends CassandraRepository<CommentContentDO, CommentContentPrimaryKey> {

    /**
     * 批量查询评论内容
     *
     * @param noteId     笔记ID
     * @param yearMonths 年月
     * @param contentIds 评论 ID列表
     * @return 评论内容
     */
    List<CommentContentDO> findByPrimaryKeyNoteIdAndPrimaryKeyYearMonthInAndPrimaryKeyContentIdIn(
            Long noteId, List<String> yearMonths, List<UUID> contentIds
    );

    /**
     * 删除评论正文
     *
     * @param noteId    笔记ID
     * @param yearMonth 年月
     * @param contentId 评论 ID
     */
    void deleteByPrimaryKeyNoteIdAndPrimaryKeyYearMonthAndPrimaryKeyContentId(Long noteId, String yearMonth, UUID contentId);
}