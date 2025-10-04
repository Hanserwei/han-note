package com.hanserwei.hannote.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.base.Preconditions;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.auth.constant.RedisKeyConstants;
import com.hanserwei.hannote.auth.enums.LoginTypeEnum;
import com.hanserwei.hannote.auth.enums.ResponseCodeEnum;
import com.hanserwei.hannote.auth.model.vo.user.UpdatePasswordReqVO;
import com.hanserwei.hannote.auth.model.vo.user.UserLoginReqVO;
import com.hanserwei.hannote.auth.rpc.UserRpcService;
import com.hanserwei.hannote.auth.service.AuthService;
import com.hanserwei.hannote.user.dto.resp.FindUserByEmailRspDTO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRpcService userRpcService;
    @Resource(name = "authTaskExecutor")
    private ThreadPoolTaskExecutor authTaskExecutor;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Response<String> loginAndRegister(UserLoginReqVO reqVO) {
        Integer loginType = reqVO.getType();
        String email = reqVO.getEmail();
        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(loginType);

        Long userId = null;

        //noinspection DataFlowIssue
        switch (loginTypeEnum) {
            case VERIFICATION_CODE:
                String verificationCode = reqVO.getCode();
                //校验参数是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(verificationCode), "验证码不能为空");

                //构建验证码的RedisKey
                String key = RedisKeyConstants.buildVerificationCodeKey(email);
                // 查询存储在 Redis 中该用户的登录验证码
                String sentCode = (String) redisTemplate.opsForValue().get(key);
                // 判断用户提交的验证码，与 Redis 中的验证码是否一致
                if (!StrUtil.equals(verificationCode, sentCode)) {
                    throw new ApiException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                }
                // RPC: 调用用户服务，注册用户
                Long userIdTmp = userRpcService.registerUser(email);

                // 若调用用户服务，返回的用户 ID 为空，则提示登录失败
                if (Objects.isNull(userIdTmp)) {
                    throw new ApiException(ResponseCodeEnum.LOGIN_FAIL);
                }

                userId = userIdTmp;
                break;
            case PASSWORD:
                String password = reqVO.getPassword();

                // RPC: 调用用户服务，通过手机号查询用户
                FindUserByEmailRspDTO findUserByEmailRspDTO = userRpcService.findUserByEmail(email);

                // 判断该手机号是否注册
                if (Objects.isNull(findUserByEmailRspDTO)) {
                    throw new ApiException(ResponseCodeEnum.USER_NOT_FOUND);
                }

                // 拿到密文密码
                String encodePassword = findUserByEmailRspDTO.getPassword();

                // 匹配密码是否一致
                boolean isPasswordCorrect = passwordEncoder.matches(password, encodePassword);

                // 如果不正确，则抛出业务异常，提示用户名或者密码不正确
                if (!isPasswordCorrect) {
                    throw new ApiException(ResponseCodeEnum.MAIL_OR_PASSWORD_ERROR);
                }

                userId = findUserByEmailRspDTO.getId();
                break;
            default:
                break;
        }
        // SaToken 登录用户，并返回 token 令牌
        StpUtil.login(userId);

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        return Response.success(tokenInfo.getTokenValue());
    }

    @Override
    public Response<?> logout() {
        Long userId = LoginUserContextHolder.getUserId();
        authTaskExecutor.submit(() -> {
            Long userId2 = LoginUserContextHolder.getUserId();
            log.info("==> 异步线程中获取 userId: {}", userId2);
        });
        StpUtil.logout(userId);
        return Response.success();
    }

    @Override
    public Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO) {
        // 新密码
        String newPassword = updatePasswordReqVO.getNewPassword();
        // 加密后的密码
        String encodePassword = passwordEncoder.encode(newPassword);
        // RPC: 调用用户服务：更新密码
        userRpcService.updatePassword(encodePassword);
        return Response.success();
    }
}
