package com.hanserwei.hannote.search.biz.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SelectMapper {


    /**
     * 查询笔记文档所需的全字段数据
     * @param noteId 笔记 ID
     * @return 笔记文档所需全字段数据
     */
    List<Map<String, Object>> selectEsNoteIndexData(@Param("noteId") Long noteId, @Param("userId") Long userId);

    /**
     * 查询用户索引所需的全字段数据
     *
     * @param userId 用户 ID
     * @return 用户索引所需全字段数据
     */
    List<Map<String, Object>> selectEsUserIndexData(@Param("userId") Long userId);
}