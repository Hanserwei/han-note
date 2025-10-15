package com.hanserwei.hannote.count.biz.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hanserwei.hannote.count.biz.domain.dataobject.NoteCountDO;
import com.hanserwei.hannote.count.biz.domain.mapper.NoteCountDOMapper;
import com.hanserwei.hannote.count.biz.service.NoteCountDOService;
@Service
public class NoteCountDOServiceImpl extends ServiceImpl<NoteCountDOMapper, NoteCountDO> implements NoteCountDOService{

}
