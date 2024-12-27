package cn.acecandy.fasaxi.eva.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * @author tangningzhu
 * @since 2024/11/25
 */
// @Configuration
@Data
@ConfigurationProperties(prefix = "emby")
public class FastEmbyConfig {
    /**
     * 内网emby地址
     */
    private String host;

    /**
     * 公网emby地址
     */
    private String publicAddr;

    /**
     * strm相关路径
     */
    private List<String> strmPaths;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}