package com.hanserwei.hannote.note.biz.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hanserwei.hannote.note.biz.domain.dataobject.NoteCollectionDO;
import com.hanserwei.hannote.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.hanserwei.hannote.note.biz.service.NoteCollectionDOService;
@Service
public class NoteCollectionDOServiceImpl extends ServiceImpl<NoteCollectionDOMapper, NoteCollectionDO> implements NoteCollectionDOService{

}
