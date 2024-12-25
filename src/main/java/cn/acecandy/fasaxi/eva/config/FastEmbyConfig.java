package cn.acecandy.fasaxi.eva.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * @author tangningzhu
 * @since 2024/11/25
 */
@Configuration
@Data
public class FastEmbyConfig {
    @Value("${emby.host}")
    public String embyHost;

    @Value("${emby.publicAddr}")
    public String embyPublicAddr;

    @Value("${emby.strmPaths}")
    public List<String> embyStrmPaths;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}