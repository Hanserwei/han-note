package com.hanserwei.hannote.data.align.domain.mapper;

import org.apache.ibatis.annotations.Param;

public interface UpdateRecordMapper {

    /**
     * 更新 t_user_count 计数表总关注数
     *
     * @param userId 用户 ID
     * @return 更新行数
     */
    int updateUserFollowingTotalByUserId(@Param("userId") long userId,
                                         @Param("followingTotal") int followingTotal);
}