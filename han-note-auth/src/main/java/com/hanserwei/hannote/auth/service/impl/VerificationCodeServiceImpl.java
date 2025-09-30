package com.hanserwei.hannote.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.auth.constant.RedisKeyConstants;
import com.hanserwei.hannote.auth.enums.ResponseCodeEnum;
import com.hanserwei.hannote.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.hanserwei.hannote.auth.service.VerificationCodeService;
import com.hanserwei.hannote.auth.utils.MailHelper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {


    private final RedisTemplate<String, Object> redisTemplate;
    private final MailHelper mailHelper;
    @Resource(name = "authTaskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 发送短信验证码！
     *
     * @param sendVerificationCodeReqVO 发送验证码VO
     * @return 响应
     */
    @Override
    public Response<?> send(SendVerificationCodeReqVO sendVerificationCodeReqVO) {
        // 邮箱
        String email = sendVerificationCodeReqVO.getEmail();
        //构建Redis的Key
        String codeKey = RedisKeyConstants.buildVerificationCodeKey(email);
        // 判断是否发送！
        Boolean hasKey = redisTemplate.hasKey(codeKey);
        if (hasKey) {
            //若之前发送的验证码未过期，则提示发送频繁
            throw new ApiException(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
        }
        //生成六位数随机验证码
        String verificationCode = RandomUtil.randomNumbers(6);
        threadPoolTaskExecutor.submit(() -> mailHelper.sendMail(verificationCode, email));
        log.info("==> 邮箱: {}, 已发送验证码：【{}】", email, verificationCode);
        // 存储验证码到 redis, 并设置过期时间为 3 分钟
        redisTemplate.opsForValue().set(codeKey, verificationCode, 3, TimeUnit.MINUTES);
        return Response.success();
    }
}
