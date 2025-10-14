package com.hanserwei.hannote.user.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.enums.DeletedEnum;
import com.hanserwei.framework.common.enums.StatusEnum;
import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.utils.JsonUtils;
import com.hanserwei.framework.common.utils.ParamUtils;
import com.hanserwei.hannote.user.biz.constant.RedisKeyConstants;
import com.hanserwei.hannote.user.biz.constant.RoleConstants;
import com.hanserwei.hannote.user.biz.domain.dataobject.RoleDO;
import com.hanserwei.hannote.user.biz.domain.dataobject.UserDO;
import com.hanserwei.hannote.user.biz.domain.dataobject.UserRoleDO;
import com.hanserwei.hannote.user.biz.domain.mapper.RoleDOMapper;
import com.hanserwei.hannote.user.biz.domain.mapper.UserDOMapper;
import com.hanserwei.hannote.user.biz.domain.mapper.UserRoleDOMapper;
import com.hanserwei.hannote.user.biz.enums.ResponseCodeEnum;
import com.hanserwei.hannote.user.biz.enums.SexEnum;
import com.hanserwei.hannote.user.biz.model.vo.UpdateUserInfoReqVO;
import com.hanserwei.hannote.user.biz.rpc.DistributedIdGeneratorRpcService;
import com.hanserwei.hannote.user.biz.rpc.OssRpcService;
import com.hanserwei.hannote.user.biz.service.UserService;
import com.hanserwei.hannote.user.dto.req.*;
import com.hanserwei.hannote.user.dto.resp.FindUserByEmailRspDTO;
import com.hanserwei.hannote.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserDOMapper, UserDO> implements UserService {

    @Resource
    private OssRpcService ossRpcService;
    @Resource
    private UserRoleDOMapper userRoleDOMapper;
    @Resource
    private RoleDOMapper roleDOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource(name = "userTaskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 用户信息本地缓存
     */
    @SuppressWarnings("NullableProblems")
    private static final Cache<Long, FindUserByIdRspDTO> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();


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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> register(RegisterUserReqDTO registerUserReqDTO) {
        String email = registerUserReqDTO.getEmail();

        // 先判断该手机号是否已被注册
        UserDO userDO1 = this.getOne(new QueryWrapper<UserDO>().eq("email", email));

        log.info("==> 用户是否注册, email: {}, userDO: {}", email, JsonUtils.toJsonString(userDO1));

        // 若已注册，则直接返回用户 ID
        if (Objects.nonNull(userDO1)) {
            return Response.success(userDO1.getId());
        }

        // 否则注册新用户
        // RPC获取全局自增的小憨书 ID
        String hanNoteId = distributedIdGeneratorRpcService.getHannoteId();
        // RPC调用获取用户ID
        String userIdStr = distributedIdGeneratorRpcService.getUserId();
        Long userId = Long.valueOf(userIdStr);

        UserDO userDO = UserDO.builder()
                .id(userId)
                .email(email)
                .hanNoteId(String.valueOf(hanNoteId)) // 自动生成小憨书号 ID
                .nickname("小憨憨" + hanNoteId) // 自动生成昵称, 如：小憨憨10000
                .status(StatusEnum.ENABLE.getValue()) // 状态为启用
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue()) // 逻辑删除
                .build();

        // 添加入库
        this.save(userDO);

        // 给该用户分配一个默认角色
        UserRoleDO userRoleDO = UserRoleDO.builder()
                .userId(userId)
                .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue())
                .build();
        userRoleDOMapper.insert(userRoleDO);

        RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);

        // 将该用户的角色 ID 存入 Redis 中
        List<String> roles = new ArrayList<>(1);
        roles.add(roleDO.getRoleKey());

        String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
        redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roles));

        return Response.success(userId);
    }

    @Override
    public Response<FindUserByEmailRspDTO> findByEmail(FindUserByEmailReqDTO findUserByEmailReqDTO) {
        String email = findUserByEmailReqDTO.getEmail();
        UserDO userDO = this.getOne(new QueryWrapper<UserDO>().eq("email", email));
        if (Objects.isNull(userDO)) {
            throw new ApiException(ResponseCodeEnum.USER_NOT_FOUND);
        }
        // 构建返参
        FindUserByEmailRspDTO findUserByEmailRspDTO = FindUserByEmailRspDTO.builder()
                .id(userDO.getId())
                .password(userDO.getPassword())
                .build();
        return Response.success(findUserByEmailRspDTO);
    }

    @Override
    public Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        // 获取当前请求对应的用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        UserDO userDO = UserDO.builder()
                .id(userId)
                .password(updateUserPasswordReqDTO.getEncodePassword()) // 加密后的密码
                .updateTime(LocalDateTime.now())
                .build();

        // 更新用户密码
        return updateById(userDO) ? Response.success() : Response.fail();
    }

    @Override
    public Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO) {
        Long userId = findUserByIdReqDTO.getId();

        // 先从本地缓存获取
        FindUserByIdRspDTO findUserByIdRspDTOLocalCache = LOCAL_CACHE.getIfPresent(userId);
        if (Objects.nonNull(findUserByIdRspDTOLocalCache)) {
            log.info("==> 本地缓存获取用户信息成功，用户 ID: {}, findUserByIdRspDTOLocalCache: {}", userId, JsonUtils.toJsonString(findUserByIdRspDTOLocalCache));
            return Response.success(findUserByIdRspDTOLocalCache);
        }

        // 用户缓存 RedisKey
        String userInfoKey = RedisKeyConstants.buildUserInfoKey(userId);
        // 先从Redis中获取
        String userInfoRedisValue = (String) redisTemplate.opsForValue().get(userInfoKey);
        // 若 Redis 中有缓存，则直接返回
        if (StringUtils.isNotBlank(userInfoRedisValue)) {
            // 将 Redis 中缓存的 JSON 字符串转为对象
            FindUserByIdRspDTO findUserByIdRspDTO = JsonUtils.parseObject(userInfoRedisValue, FindUserByIdRspDTO.class);
            // 异步缓存到本地缓存中
            threadPoolTaskExecutor.submit(() -> {
                // 缓存到本地缓存中
                if (findUserByIdRspDTO != null) {
                    LOCAL_CACHE.put(userId, findUserByIdRspDTO);
                }
            });
            return Response.success(findUserByIdRspDTO);
        }
        // 若 Redis 中没有缓存，则从数据库中获取
        UserDO userDO = this.getOne(new QueryWrapper<UserDO>().eq("id", userId));

        // 判空
        if (Objects.isNull(userDO)) {
            threadPoolTaskExecutor.submit(() -> {
                // 防止缓存击穿，缓存空对象
                // 过期时间保底1分钟+随机秒数，避免缓存雪崩
                long expireTime = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(userInfoKey, "null", expireTime, TimeUnit.SECONDS);
            });
            throw new ApiException(ResponseCodeEnum.USER_NOT_FOUND);
        }
        // 构建返参
        FindUserByIdRspDTO findUserByIdRspDTO = FindUserByIdRspDTO.builder()
                .id(userDO.getId())
                .nickName(userDO.getNickname())
                .avatar(userDO.getAvatar())
                .introduction(userDO.getIntroduction())
                .build();
        threadPoolTaskExecutor.submit(() -> {
            // 过期时间保底1天+随机秒数，避免缓存雪崩
            long expireTime = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            redisTemplate.opsForValue().set(userInfoKey, JsonUtils.toJsonString(findUserByIdRspDTO), expireTime, TimeUnit.SECONDS);
        });
        return Response.success(findUserByIdRspDTO);
    }

    @Override
    public Response<List<FindUserByIdRspDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
        List<Long> userIds = findUsersByIdsReqDTO.getIds();

        // 构建Redis的Key集合
        List<String> userInfoKeys = userIds.stream()
                .map(RedisKeyConstants::buildUserInfoKey)
                .toList();

        // 先从Redis中获取
        List<Object> redisValues = redisTemplate.opsForValue().multiGet(userInfoKeys);

        // 如果缓存中不为空，过滤掉空值
        if (CollUtil.isNotEmpty(redisValues)){
            redisValues = redisValues.stream()
                    .filter(Objects::nonNull)
                    .toList();
        }

        // 返参
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = Lists.newArrayList();

        // 将过滤后的缓存集合，转换为 DTO 返参实体类
        if (CollUtil.isNotEmpty(redisValues)) {
            findUserByIdRspDTOS = redisValues.stream()
                    .map(value -> JsonUtils.parseObject(String.valueOf(value), FindUserByIdRspDTO.class))
                    .collect(Collectors.toList());
        }

        // 如果被查询的用户信息，都在 Redis 缓存中, 则直接返回
        if (CollUtil.size(userIds) == CollUtil.size(findUserByIdRspDTOS)) {
            return Response.success(findUserByIdRspDTOS);
        }

        // 还有另外两种情况：一种是缓存里没有用户信息数据，还有一种是缓存里数据不全，需要从数据库中补充
        // 筛选出缓存里没有的用户数据，去查数据库
        List<Long> userIdsNeedQuery = null;
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
            // 将 findUserInfoByIdRspDTOS 集合转 Map
            Map<Long, FindUserByIdRspDTO> map = findUserByIdRspDTOS.stream()
                    .collect(Collectors.toMap(FindUserByIdRspDTO::getId, p -> p));

            // 筛选出需要查 DB 的用户 ID
            userIdsNeedQuery = userIds.stream()
                    .filter(id -> Objects.isNull(map.get(id)))
                    .toList();
        } else { // 缓存中一条用户信息都没查到，则提交的用户 ID 集合都需要查数据库
            userIdsNeedQuery = userIds;
        }

        // 数据库中批量查询用户信息
        List<UserDO> userDOs = this.list(new LambdaQueryWrapper<>(UserDO.class)
                .eq(UserDO::getStatus, 0)
                .eq(UserDO::getIsDeleted, 0)
                .in(UserDO::getId, userIdsNeedQuery));
        List<FindUserByIdRspDTO> findUserByIdRspDTOS2 = null;
        // 若数据不为空，则转为 DTO 返回
        if (CollUtil.isNotEmpty(userDOs)){
            findUserByIdRspDTOS2 = userDOs.stream()
                    .map(userDO -> FindUserByIdRspDTO.builder()
                            .id(userDO.getId())
                            .nickName(userDO.getNickname())
                            .introduction(userDO.getIntroduction())
                            .avatar(userDO.getAvatar())
                            .build())
                    .toList();
            // 批量更新 Redis
            List<FindUserByIdRspDTO> finalFindUserByIdRspDTOS = findUserByIdRspDTOS2;
            threadPoolTaskExecutor.submit(()->{
               // DTO集合转Map
                Map<Long, FindUserByIdRspDTO> map = finalFindUserByIdRspDTOS.stream()
                        .collect(Collectors.toMap(FindUserByIdRspDTO::getId, p -> p));

                // 执行pipeline操作
                //noinspection NullableProblems
                redisTemplate.executePipelined(new SessionCallback<>() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public Object execute(RedisOperations operations) throws DataAccessException {

                        //    你可以直接获取 ValueOperations
                        ValueOperations<String, Object> valueOperations = operations.opsForValue();

                        for (UserDO userDO : userDOs) {
                            Long userId = userDO.getId();

                            // 用户缓存Key (K = String)
                            String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);

                            // DTO转JSON
                            FindUserByIdRspDTO findUserByIdRspDTO = map.get(userId);

                            //    的值类型是 Object，所以它可以接受 String。
                            String value = JsonUtils.toJsonString(findUserByIdRspDTO);

                            // 过期时间
                            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                            valueOperations.set(userInfoRedisKey, value, expireSeconds, TimeUnit.SECONDS);
                        }
                        return null;
                    }
                });
            });
        }
        // 合并数据
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS2)) {
            findUserByIdRspDTOS.addAll(findUserByIdRspDTOS2);
        }

        return Response.success(findUserByIdRspDTOS);
    }
}

