package com.hanserwei.hannote.count.biz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hanserwei.hannote.count.biz.domain.dataobject.NoteCountDO;
import com.hanserwei.hannote.count.biz.domain.mapper.NoteCountDOMapper;
import com.hanserwei.hannote.count.biz.service.NoteCountDOService;
import org.springframework.stereotype.Service;
@Service
public class NoteCountDOServiceImpl extends ServiceImpl<NoteCountDOMapper, NoteCountDO> implements NoteCountDOService{

}
