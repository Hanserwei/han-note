package com.hanserwei.hannote.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.auth.domain.dataobject.UserDO;
import com.hanserwei.hannote.auth.model.vo.user.UserLoginReqVO;

public interface UserService extends IService<UserDO> {

    /**
     * 登录与注册
     *
     * @param reqVO 请求参数
     * @return 响应结果
     */
    Response<String> loginAndRegister(UserLoginReqVO reqVO);
}
