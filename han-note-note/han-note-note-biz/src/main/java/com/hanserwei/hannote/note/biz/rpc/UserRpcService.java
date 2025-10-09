package com.hanserwei.hannote.note.biz.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.user.api.UserFeignApi;
import com.hanserwei.hannote.user.dto.req.FindUserByIdReqDTO;
import com.hanserwei.hannote.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UserRpcService {
    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public FindUserByIdRspDTO findById(Long userId) {
        FindUserByIdReqDTO findUserByIdReqDTO = new FindUserByIdReqDTO();
        findUserByIdReqDTO.setId(userId);

        Response<FindUserByIdRspDTO> response = userFeignApi.findById(findUserByIdReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()) {
            return null;
        }

        return response.getData();
    }

}

