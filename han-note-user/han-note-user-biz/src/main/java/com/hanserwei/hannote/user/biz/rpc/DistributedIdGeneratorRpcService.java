package com.hanserwei.hannote.user.biz.rpc;

import com.hanserwei.hannote.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DistributedIdGeneratorRpcService {

    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    /**
     * Leaf 号段模式：小憨书 ID 业务标识
     */
    private static final String BIZ_TAG_HANNOTE_ID = "leaf-segment-hannote-id";

    /**
     * Leaf 号段模式：用户 ID 业务标识
     */
    private static final String BIZ_TAG_USER_ID = "leaf-segment-user-id";

    /**
     * 调用分布式 ID 生成服务生成小憨书 ID
     *
     * @return 小憨书 ID
     */
    public String getHannoteId() {
        return distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_HANNOTE_ID);
    }

    /**
     * 调用分布式 ID 生成服务用户 ID
     *
     * @return 用户 ID
     */
    public String getUserId() {
        return distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_USER_ID);
    }
}