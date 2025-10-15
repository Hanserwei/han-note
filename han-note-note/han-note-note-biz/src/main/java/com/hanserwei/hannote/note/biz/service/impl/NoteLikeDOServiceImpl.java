package com.hanserwei.hannote.note.biz.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hanserwei.hannote.note.biz.domain.mapper.NoteLikeDOMapper;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteLikeDO;
import com.hanserwei.hannote.note.biz.service.NoteLikeDOService;
@Service
public class NoteLikeDOServiceImpl extends ServiceImpl<NoteLikeDOMapper, NoteLikeDO> implements NoteLikeDOService{

}
