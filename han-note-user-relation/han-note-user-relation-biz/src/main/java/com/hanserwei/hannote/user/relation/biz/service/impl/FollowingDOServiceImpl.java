package com.hanserwei.hannote.user.relation.biz.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hanserwei.hannote.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.hanserwei.hannote.user.relation.biz.domain.dataobject.FollowingDO;
import com.hanserwei.hannote.user.relation.biz.service.FollowingDOService;
@Service
public class FollowingDOServiceImpl extends ServiceImpl<FollowingDOMapper, FollowingDO> implements FollowingDOService{

}
