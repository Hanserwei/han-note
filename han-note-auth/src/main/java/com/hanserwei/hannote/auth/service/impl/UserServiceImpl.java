package com.hanserwei.hannote.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Preconditions;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.enums.DeletedEnum;
import com.hanserwei.framework.common.enums.StatusEnum;
import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.hannote.auth.constant.RedisKeyConstants;
import com.hanserwei.hannote.auth.constant.RoleConstants;
import com.hanserwei.hannote.auth.domain.dataobject.RoleDO;
import com.hanserwei.hannote.auth.domain.dataobject.UserDO;
import com.hanserwei.hannote.auth.domain.dataobject.UserRoleDO;
import com.hanserwei.hannote.auth.domain.mapper.RoleDOMapper;
import com.hanserwei.hannote.auth.domain.mapper.UserDOMapper;
import com.hanserwei.hannote.auth.domain.mapper.UserRoleDOMapper;
import com.hanserwei.hannote.auth.enums.LoginTypeEnum;
import com.hanserwei.hannote.auth.enums.ResponseCodeEnum;
import com.hanserwei.hannote.auth.model.vo.user.UpdatePasswordReqVO;
import com.hanserwei.hannote.auth.model.vo.user.UserLoginReqVO;
import com.hanserwei.hannote.auth.service.UserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserDOMapper, UserDO> implements UserService {


    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRoleDOMapper userRoleDOMapper;
    private final TransactionTemplate transactionTemplate;
    private final RoleDOMapper roleDOMapper;
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
                //通过邮箱查询用户
                UserDO userDO = this.getOne(new QueryWrapper<UserDO>().eq("email", email));
                log.info("==> 用户是否注册, email: {}, userDO: {}", email, JsonUtils.toJsonString(userDO));
                // 判断是否注册
                if (Objects.isNull(userDO)) {
                    // 若此用户还没有注册，系统自动注册该用户
                    userId = registerUser(email);
                } else {
                    // 已注册，则获取其用户 ID
                    userId = userDO.getId();
                }
                break;
            case PASSWORD:
                String password = reqVO.getPassword();
                // 根据邮箱号查询
                UserDO userDO1 = this.getOne(new QueryWrapper<UserDO>().eq("email", email));
                if (Objects.isNull(userDO1)){
                    throw new ApiException(ResponseCodeEnum.USER_NOT_FOUND);
                }
                // 拿到密文密码
                String encodePassword = userDO1.getPassword();
                boolean isPasswordCorrect = passwordEncoder.matches(password, encodePassword);
                if (!isPasswordCorrect) {
                    throw new ApiException(ResponseCodeEnum.MAIL_OR_PASSWORD_ERROR);
                }
                userId = userDO1.getId();
                break;
            default:
                break;
        }
        // SaToken 登录用户，并返回 token 令牌
        StpUtil.login(userId);

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        return Response.success(tokenInfo.getTokenValue());
    }

    public Long registerUser(String email) {
        return transactionTemplate.execute(status -> {
            try {
                // 获取全局自增的小憨书ID
                Long hanNoteId = redisTemplate.opsForValue().increment(RedisKeyConstants.HAN_NOTE_ID_GENERATOR_KEY);
                UserDO userDO = UserDO.builder()
                        .hanNoteId(String.valueOf(hanNoteId)) // 自动生成小红书号 ID
                        .nickname("小憨憨" + hanNoteId) // 自动生成昵称, 如：小憨憨10000
                        .status(StatusEnum.ENABLE.getValue()) // 状态为启用
                        .email(email)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeletedEnum.NO.getValue()) // 逻辑删除
                        .build();

                // 添加入库
                this.save(userDO);

                // 获取入库的用户ID
                Long userId = userDO.getId();

                // 添加默认用户角色
                UserRoleDO userRoleDO = UserRoleDO.builder()
                        .userId(userId)
                        .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeletedEnum.NO.getValue())
                        .build();
                userRoleDOMapper.insert(userRoleDO);

                RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);

                // 将该用户的角色 ID 存入 Redis 中，指定初始容量为 1，这样可以减少在扩容时的性能开销
                List<String> roles = new ArrayList<>(1);
                roles.add(roleDO.getRoleKey());

                String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
                redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roles));

                return userId;
            } catch (Exception e) {
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("==> 系统注册用户异常: ", e);
                return null;
            }
        });
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
        // 获取用户ID
        Long userId = LoginUserContextHolder.getUserId();

        UserDO userDO = UserDO.builder()
                .id(userId)
                .password(encodePassword)
                .updateTime(LocalDateTime.now())
                .build();
        // 更新用户密码
        this.updateById(userDO);
        return Response.success();
    }
}
