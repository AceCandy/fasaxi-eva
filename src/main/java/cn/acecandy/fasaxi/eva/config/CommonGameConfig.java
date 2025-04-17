package cn.acecandy.fasaxi.eva.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 通用游戏配置
 *
 * @author tangningzhu
 * @since 2024/11/25
 */
@Configuration
@ConfigurationProperties(prefix = "common-game")
@Data
public class CommonGameConfig {
    /**
     * 看图猜番号
     */
    private Gx ktcfh;
    /**
     * 看图猜成语
     */
    private Gx ktccy;

    @Data
    public static class Gx {
        /**
         * 定时任务开关
         */
        private Boolean enable;
        /**
         * 路径
         */
        private String path;
    }
}