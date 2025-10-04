package com.hanserwei.hannote.user.biz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Preconditions;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.utils.ParamUtils;
import com.hanserwei.hannote.user.biz.domain.dataobject.UserDO;
import com.hanserwei.hannote.user.biz.domain.mapper.UserDOMapper;
import com.hanserwei.hannote.user.biz.enums.ResponseCodeEnum;
import com.hanserwei.hannote.user.biz.enums.SexEnum;
import com.hanserwei.hannote.user.biz.model.vo.UpdateUserInfoReqVO;
import com.hanserwei.hannote.user.biz.rpc.OssRpcService;
import com.hanserwei.hannote.user.biz.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserDOMapper, UserDO> implements UserService {

    @Resource
    private OssRpcService ossRpcService;

    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        UserDO userDO = new UserDO();
        // 设置当前需要更新的用户 ID
        userDO.setId(LoginUserContextHolder.getUserId());
        // 标识位：是否需要更新
        boolean needUpdate = false;

        // 头像
        MultipartFile avatar = updateUserInfoReqVO.getAvatar();

        if (Objects.nonNull(avatar)) {
            String avatarUrl = ossRpcService.uploadFile(avatar);
            log.info("==> 调用 oss 服务成功，上传头像，url：{}", avatarUrl);

            // 若上传头像失败，则抛出业务异常
            if (StringUtils.isBlank(avatarUrl)) {
                throw new ApiException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
            }

            userDO.setAvatar(avatarUrl);
            needUpdate = true;
        }

        // 昵称
        String nickname = updateUserInfoReqVO.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            Preconditions.checkArgument(ParamUtils.checkNickname(nickname), ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMsg());
            userDO.setNickname(nickname);
            needUpdate = true;
        }

        // 小憨书 ID
        String hanNoteId = updateUserInfoReqVO.getHanNoteId();
        if (StringUtils.isNotBlank(hanNoteId)) {
            Preconditions.checkArgument(ParamUtils.checkHannoteId(hanNoteId), ResponseCodeEnum.HAN_NOTE_ID_VALID_FAIL.getErrorMsg());
            userDO.setHanNoteId(hanNoteId);
            needUpdate = true;
        }

        // 性别
        Integer sex = updateUserInfoReqVO.getSex();
        if (Objects.nonNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL.getErrorMsg());
            userDO.setSex(sex);
            needUpdate = true;
        }

        // 生日
        LocalDate birthday = updateUserInfoReqVO.getBirthday();
        if (Objects.nonNull(birthday)) {
            userDO.setBirthday(birthday);
            needUpdate = true;
        }

        // 个人介绍
        String introduction = updateUserInfoReqVO.getIntroduction();
        if (StringUtils.isNotBlank(introduction)) {
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100), ResponseCodeEnum.INTRODUCTION_VALID_FAIL.getErrorMsg());
            userDO.setIntroduction(introduction);
            needUpdate = true;
        }

        // 背景图片
        MultipartFile backgroundImg = updateUserInfoReqVO.getBackgroundImg();
        if (Objects.nonNull(backgroundImg)) {
            String backgroundImgUrl = ossRpcService.uploadFile(backgroundImg);
            log.info("==> 调用 oss 服务成功，上传背景图，url：{}", backgroundImg);

            // 若上传背景图失败，则抛出业务异常
            if (StringUtils.isBlank(backgroundImgUrl)) {
                throw new ApiException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
            }

            userDO.setBackgroundImg(backgroundImgUrl);
            needUpdate = true;
        }

        if (needUpdate) {
            userDO.setUpdateTime(LocalDateTime.now());
            return updateById(userDO) ? Response.success() : Response.fail();
        }
        return Response.success();
    }
}
