package com.hanserwei.hannote.count.biz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hanserwei.hannote.count.biz.domain.dataobject.UserCountDO;
import com.hanserwei.hannote.count.biz.domain.mapper.UserCountDOMapper;
import com.hanserwei.hannote.count.biz.service.UserCountDOService;
import org.springframework.stereotype.Service;
@Service
public class UserCountDOServiceImpl extends ServiceImpl<UserCountDOMapper, UserCountDO> implements UserCountDOService{

}
