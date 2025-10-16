package com.hanserwei.hannote.count.biz.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.hannote.count.biz.domain.dataobject.UserCountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserCountDOMapper extends BaseMapper<UserCountDO> {

    /**
     * 添加或更新粉丝总数
     *
     * @param count  粉丝数
     * @param userId 用户ID
     * @return 影响行数
     */
    int insertOrUpdateFansTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    /**
     * 添加或更新关注总数
     *
     * @param count  关注数
     * @param userId 用户ID
     * @return 影响行数
     */
    int insertOrUpdateFollowingTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);
}