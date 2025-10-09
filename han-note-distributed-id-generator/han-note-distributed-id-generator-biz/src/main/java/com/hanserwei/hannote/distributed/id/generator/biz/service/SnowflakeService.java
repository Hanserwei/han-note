package com.hanserwei.hannote.distributed.id.generator.biz.service;

import com.hanserwei.hannote.distributed.id.generator.biz.config.LeafProperties;
import com.hanserwei.hannote.distributed.id.generator.biz.core.IDGen;
import com.hanserwei.hannote.distributed.id.generator.biz.core.common.Result;
import com.hanserwei.hannote.distributed.id.generator.biz.core.common.ZeroIDGen;
import com.hanserwei.hannote.distributed.id.generator.biz.core.snowflake.SnowflakeIDGenImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnowflakeService {

    private final LeafProperties leafProperties;

    private IDGen idGen;

    @PostConstruct
    public void init() {
        if (leafProperties.getSnowflake().isEnable()) {
            String zkAddress = leafProperties.getSnowflake().getZkAddress();
            if (!StringUtils.hasText(zkAddress)) {
                throw new IllegalStateException("Snowflake Service Init Fail: zk address is required");
            }
            int port = leafProperties.getSnowflake().getPort();
            if (port <= 0) {
                throw new IllegalStateException("Snowflake Service Init Fail: port must be positive");
            }
            SnowflakeIDGenImpl snowflakeIDGen = new SnowflakeIDGenImpl(leafProperties.getName(), zkAddress, port);
            if (snowflakeIDGen.init()) {
                this.idGen = snowflakeIDGen;
                log.info("Snowflake Service Init Successfully with zkAddress={} and port={}", zkAddress, port);
            } else {
                throw new IllegalStateException("Snowflake Service Init Fail");
            }
        } else {
            this.idGen = new ZeroIDGen();
            log.info("Snowflake Service disabled, Zero ID Gen Service Init Successfully");
        }
    }

    public Result getId(String key) {
        if (idGen == null) {
            throw new IllegalStateException("Snowflake Service not initialized");
        }
        return idGen.get(key);
    }
}
