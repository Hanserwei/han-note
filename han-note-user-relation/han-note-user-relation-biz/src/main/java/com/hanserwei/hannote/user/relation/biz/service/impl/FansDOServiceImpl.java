package com.hanserwei.hannote.user.relation.biz.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hanserwei.hannote.user.relation.biz.domain.dataobject.FansDO;
import com.hanserwei.hannote.user.relation.biz.domain.mapper.FansDOMapper;
import com.hanserwei.hannote.user.relation.biz.service.FansDOService;
@Service
public class FansDOServiceImpl extends ServiceImpl<FansDOMapper, FansDO> implements FansDOService{

}
