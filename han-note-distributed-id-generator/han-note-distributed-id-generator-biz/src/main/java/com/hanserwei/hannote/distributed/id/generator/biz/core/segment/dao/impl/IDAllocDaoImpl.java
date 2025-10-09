package com.hanserwei.hannote.distributed.id.generator.biz.core.segment.dao.impl;

import com.hanserwei.hannote.distributed.id.generator.biz.core.segment.dao.IDAllocDao;
import com.hanserwei.hannote.distributed.id.generator.biz.core.segment.dao.IDAllocMapper;
import com.hanserwei.hannote.distributed.id.generator.biz.core.segment.model.LeafAlloc;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
public class IDAllocDaoImpl implements IDAllocDao {

    private final IDAllocMapper idAllocMapper;
    @PostConstruct
    void logInit() {
        log.info("IDAllocDaoImpl initialized as primary IDAllocDao implementation");
    }

    @Override
    public List<LeafAlloc> getAllLeafAllocs() {
        return idAllocMapper.getAllLeafAllocs();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeafAlloc updateMaxIdAndGetLeafAlloc(String tag) {
        idAllocMapper.updateMaxId(tag);
        return idAllocMapper.getLeafAlloc(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeafAlloc updateMaxIdByCustomStepAndGetLeafAlloc(LeafAlloc leafAlloc) {
        idAllocMapper.updateMaxIdByCustomStep(leafAlloc);
        return idAllocMapper.getLeafAlloc(leafAlloc.getKey());
    }

    @Override
    public List<String> getAllTags() {
        return idAllocMapper.getAllTags();
    }
}
