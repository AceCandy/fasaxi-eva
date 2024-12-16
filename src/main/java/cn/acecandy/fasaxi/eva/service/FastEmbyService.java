package cn.acecandy.fasaxi.eva.service;

import cn.acecandy.fasaxi.eva.bot.impl.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.config.FastEmbyConfig;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * FastEmby服务
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class FastEmbyService {

    @Resource
    private EmbyTelegramBot tgBot;

    @Resource
    private FastEmbyConfig fastEmbyConfig;

    @Resource
    private RestTemplate restTemplate;

    public ResponseEntity<?> handleEmbyRequest(String path, HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        log.info("Received request: " + request.getMethod() + " " + requestUrl);

        String originalUrl = fastEmbyConfig.getEmbyHost() + "/emby/" + path;
        return proxyRequest(originalUrl, request);
    }

    public ResponseEntity<?> handleVideosRequest(String path, HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        log.info("Received request: " + request.getMethod() + " " + requestUrl);

        String originalUrl = fastEmbyConfig.getEmbyHost() + "/Videos/" + path;
        return proxyRequest(originalUrl, request);
    }

    private ResponseEntity<?> proxyRequest(String originalUrl, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAll(getHeadersMap(request));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(originalUrl, HttpMethod.GET, entity, String.class);

            return ResponseEntity.status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());
        } catch (Exception e) {
            log.error("Proxy error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Proxy error: " + e.getMessage());
        }
    }

    private Map<String, String> getHeadersMap(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        for (String headerName : Collections.list(request.getHeaderNames())) {
            if (!"host".equalsIgnoreCase(headerName)) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        return headers;
    }
}