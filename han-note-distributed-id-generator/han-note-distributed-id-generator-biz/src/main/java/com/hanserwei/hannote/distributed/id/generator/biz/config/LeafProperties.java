package com.hanserwei.hannote.distributed.id.generator.biz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "leaf")
public class LeafProperties {

    /**
     * 用于区分不同集群的唯一名称，影响 Snowflake 的 zk 节点路径。
     */
    private String name = "leaf";

    private final Segment segment = new Segment();
    private final Snowflake snowflake = new Snowflake();
    private final Jdbc jdbc = new Jdbc();

    @Getter
    @Setter
    public static class Segment {
        /**
         * 是否启用号段模式 ID 生成。
         */
        private boolean enable = true;
    }

    @Getter
    @Setter
    public static class Snowflake {
        /**
         * 是否启用 Snowflake 模式 ID 生成。
         */
        private boolean enable = true;

        /**
         * Zookeeper 连接地址，示例：127.0.0.1:2181。
         */
        private String zkAddress;

        /**
         * Snowflake 服务监听端口。
         */
        private int port = 0;
    }

    @Getter
    @Setter
    public static class Jdbc {
        /**
         * JDBC 驱动类名。
         */
        private String driverClassName = "com.mysql.cj.jdbc.Driver";

        /**
         * 数据库连接 URL。
         */
        private String url;

        /**
         * 数据库用户名。
         */
        private String username;

        /**
         * 数据库密码。
         */
        private String password;
    }
}
