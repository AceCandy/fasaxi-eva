package cn.acecandy.fasaxi.eva.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 通用配置类
 *
 * @author tangningzhu
 * @since 2024/11/22
 */
@Configuration
public class CommonConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}