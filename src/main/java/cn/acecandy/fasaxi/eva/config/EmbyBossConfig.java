package cn.acecandy.fasaxi.eva.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author tangningzhu
 * @since 2024/11/25
 */
@Configuration
@ConfigurationProperties(prefix = "tg-bot.emby-boss")
@Data
public class EmbyBossConfig {
    /**
     * 机器人名称
     */
    private String name;
    /**
     * 机器人token
     */
    private String token;

    /**
     * 管理员 可多个,逗号分隔
     */
    private String admins;
    /**
     * 拥有者 可多个,逗号分隔
     */
    private String owners;
    /**
     * 群组 暂时只允许一个
     */
    private String group;
}