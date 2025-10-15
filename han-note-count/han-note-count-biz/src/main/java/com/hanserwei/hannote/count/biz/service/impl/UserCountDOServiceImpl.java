package com.hanserwei.hannote.count.biz.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hanserwei.hannote.count.biz.domain.mapper.UserCountDOMapper;
import com.hanserwei.hannote.count.biz.domain.dataobject.UserCountDO;
import com.hanserwei.hannote.count.biz.service.UserCountDOService;
@Service
public class UserCountDOServiceImpl extends ServiceImpl<UserCountDOMapper, UserCountDO> implements UserCountDOService{

}
