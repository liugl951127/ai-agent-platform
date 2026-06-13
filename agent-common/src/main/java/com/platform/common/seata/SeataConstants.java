package com.platform.common.seata;

/**
 * Seata 分布式事务常量
 */
public final class SeataConstants {
    private SeataConstants() {}

    /** 默认事务组(需与各服务 application.yml 中 seata.tx-service-group 一致) */
    public static final String DEFAULT_TX_GROUP = "agent-platform-tx-group";

    /** Seata 服务端地址(默认从 nacos 配置中心拉取) */
    public static final String DEFAULT_GRP_ID = "agent-platform-seata";
}
