package com.hanserwei.hannote.search.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SelectMapper {

    /**
     * 查询笔记文档所需的全字段数据
     *
     * @param noteId 笔记 ID
     * @return 笔记文档所需全字段数据
     */
    List<Map<String, Object>> selectEsNoteIndexData(@Param("noteId") long noteId);
}