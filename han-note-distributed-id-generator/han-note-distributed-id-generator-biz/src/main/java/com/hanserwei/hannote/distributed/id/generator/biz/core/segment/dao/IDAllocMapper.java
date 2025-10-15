package com.hanserwei.hannote.distributed.id.generator.biz.core.segment.dao;

import com.hanserwei.hannote.distributed.id.generator.biz.core.segment.model.LeafAlloc;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface IDAllocMapper {

    @Select("SELECT biz_tag, max_id, step, update_time FROM leaf")
    @Results(value = {
            @Result(column = "biz_tag", property = "key"),
            @Result(column = "max_id", property = "maxId"),
            @Result(column = "step", property = "step"),
            @Result(column = "update_time", property = "updateTime")
    })
    List<LeafAlloc> getAllLeafAllocs();

    @Select("SELECT biz_tag, max_id, step FROM leaf WHERE biz_tag = #{tag}")
    @Results(value = {
            @Result(column = "biz_tag", property = "key"),
            @Result(column = "max_id", property = "maxId"),
            @Result(column = "step", property = "step")
    })
    LeafAlloc getLeafAlloc(@Param("tag") String tag);

    @Update("UPDATE leaf SET max_id = max_id + step WHERE biz_tag = #{tag}")
    void updateMaxId(@Param("tag") String tag);

    @Update("UPDATE leaf SET max_id = max_id + #{leafAlloc.step} WHERE biz_tag = #{leafAlloc.key}")
    void updateMaxIdByCustomStep(@Param("leafAlloc") LeafAlloc leafAlloc);

    @Select("SELECT biz_tag FROM leaf")
    List<String> getAllTags();
}
