package com.hanserwei.hannote.search.controller;

import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.hannote.search.model.vo.SearchUserReqVO;
import com.hanserwei.hannote.search.model.vo.SearchUserRspVO;
import com.hanserwei.hannote.search.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/user")
    @ApiOperationLog(description = "搜索用户")
    public PageResponse<SearchUserRspVO> searchUser(@RequestBody @Validated SearchUserReqVO searchUserReqVO) {
        return userService.searchUser(searchUserReqVO);
    }

}
