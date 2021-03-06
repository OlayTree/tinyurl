package com.github.tinyurl.service.impl;

/**
 * snowflake 有序ID生成器
 * @see http://github.com/twitter/snowflake
 *
 * @author errorfatal89@gmail.com
 */
import com.github.tinyurl.config.SnowflakeConfig;
import com.github.tinyurl.dao.UrlDao;
import com.github.tinyurl.domain.model.UrlModel;
import com.github.tinyurl.domain.request.ShortenRequest;
import com.github.tinyurl.service.UidGenerator;
import com.github.tinyurl.service.UidGeneratorParam;
import com.github.tinyurl.service.UidObject;
import com.github.tinyurl.util.Md5Util;
import com.github.tinyurl.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Slf4j
@Service("snowflakeUidGenerator")
public class SnowflakeUidGenerator implements UidGenerator {

    private static final long EPOCH = 1594720861895L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);
    private static final long WORKER_ID_MASK = -1L ^ (-1L << WORKER_ID_BITS);
    private static final long DATACENTER_ID_MASK = -1L ^ (-1L << DATACENTER_ID_BITS);

    private long workerId;
    private long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    @Resource
    private SnowflakeConfig snowflakeConfig;

    @Resource
    private UrlDao urlDao;


    public SnowflakeUidGenerator() {}

    @Bean(initMethod = "initialize")
    @DependsOn("snowflakeConfig")
    public void initialize() {
        this.datacenterId = snowflakeConfig.getDataCenterId() & DATACENTER_ID_MASK;
        this.workerId = snowflakeConfig.getWorkerId() & WORKER_ID_MASK;
    }

    public synchronized long nextId() {
        long timestamp = timestamp();

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        if (timestamp < lastTimestamp) {
            log.error("Clock is moving backwards. Rejecting requests until " + lastTimestamp + ".");
            throw new IllegalStateException("Clock moved backwards. Refusing to generate id for " + (lastTimestamp - timestamp) + " milliseconds");
        }

        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = timestamp();
        }
        return timestamp;
    }

    protected long timestamp() {
        return System.currentTimeMillis();
    }

    @Override
    public UidObject generate(UidGeneratorParam param) {
        long id = nextId();
        ShortenRequest request = (ShortenRequest) param;
        UrlModel urlModel = new UrlModel();
        urlModel.setCreateTime(new Date());
        urlModel.setOriginUrl(request.getUrl());
        urlModel.setHash(Md5Util.encode(request.getUrl(), StringUtil.EMPTY));
        urlModel.setId(id);
        urlDao.insertWithId(urlModel);
        return new SnowflakeUidObject(id);
    }
}
