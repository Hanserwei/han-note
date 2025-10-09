package com.hanserwei.hannote.distributed.id.generator.biz.service;

import com.hanserwei.hannote.distributed.id.generator.biz.config.LeafProperties;
import com.hanserwei.hannote.distributed.id.generator.biz.core.IDGen;
import com.hanserwei.hannote.distributed.id.generator.biz.core.common.Result;
import com.hanserwei.hannote.distributed.id.generator.biz.core.common.ZeroIDGen;
import com.hanserwei.hannote.distributed.id.generator.biz.core.segment.SegmentIDGenImpl;
import com.hanserwei.hannote.distributed.id.generator.biz.core.segment.dao.IDAllocDao;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentService {

    private final LeafProperties leafProperties;
    private final IDAllocDao idAllocDao;

    private IDGen idGen;

    @PostConstruct
    public void init() {
        if (leafProperties.getSegment().isEnable()) {
            SegmentIDGenImpl segmentIDGen = new SegmentIDGenImpl();
            segmentIDGen.setDao(idAllocDao);
            if (segmentIDGen.init()) {
                this.idGen = segmentIDGen;
                log.info("Segment Service Init Successfully");
            } else {
                throw new IllegalStateException("Segment Service Init Fail");
            }
        } else {
            this.idGen = new ZeroIDGen();
            log.info("Segment Service disabled, Zero ID Gen Service Init Successfully");
        }
    }

    public Result getId(String key) {
        if (idGen == null) {
            throw new IllegalStateException("Segment Service not initialized");
        }
        return idGen.get(key);
    }

    public SegmentIDGenImpl getIdGen() {
        return idGen instanceof SegmentIDGenImpl ? (SegmentIDGenImpl) idGen : null;
    }
}
