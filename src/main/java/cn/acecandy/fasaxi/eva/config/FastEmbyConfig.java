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
    // @Value("${emby.host}")
    private String host;

    // @Value("${emby.publicAddr}")
    private String publicAddr;

    // @Value("${emby.strm-paths}")
    private List<String> strmPaths;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}