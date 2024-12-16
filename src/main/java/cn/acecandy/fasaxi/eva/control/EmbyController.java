package cn.acecandy.fasaxi.eva.control;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.acecandy.fasaxi.eva.bean.req.VideoRedirectReq;
import cn.acecandy.fasaxi.eva.service.EmbyService;
import cn.acecandy.fasaxi.eva.utils.EmbyUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;

@Slf4j
@Tag(name = "Emby处理")
@RestController
@RequestMapping("/")
public class EmbyController {

    @Resource
    private EmbyService embyService;

    /*@Resource
    private RestTemplate restTemplate;

    private final String backendServiceUrl = "http://worldline-real.acecandy.cn:800";  // 后端服务 URL

    @SneakyThrows
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyRequest(HttpServletRequest request) {
        // 获取请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Host", "worldline-real.acecandy.cn");
        // headers.set("Host", request.getHeader("Host"));  // 保留原始 Host
        headers.set("X-Real-IP", request.getRemoteAddr());  // 客户端真实 IP
        headers.set("X-Forwarded-For", request.getHeader("X-Forwarded-For"));  // 转发的 IP 列表
        headers.set("X-Forwarded-Proto", request.getScheme());  // 原请求协议
        // 其他需要的请求头可以添加到这里

        // 构建请求实体
        HttpEntity<byte[]> entity = new HttpEntity<>(headers);
        // HttpEntity<byte[]> entity;
        if (request.getMethod().equalsIgnoreCase("POST")
                || request.getMethod().equalsIgnoreCase("PUT")) {
            byte[] body = request.getInputStream().readAllBytes();
            entity = new HttpEntity<>(body, headers);
        } else {
            entity = new HttpEntity<>(headers);
        }

        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();  // 获取请求的查询参数
        String url = backendServiceUrl;
        if (!StrUtil.startWith(requestUri, "/emby/")) {
            url = url + "/web" + requestUri;
        } else {
            url = url + requestUri;
        }
        url = url + (queryString != null ? "?" + queryString : "");

        // 转发请求并返回响应
        *//*ResponseEntity<String> responseEntity = restTemplate.exchange(url,
                HttpMethod.valueOf(request.getMethod()), entity, String.class);*//*
        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(url, HttpMethod.valueOf(request.getMethod()), entity, byte[].class);


        return ResponseEntity.status(responseEntity.getStatusCode())
                .headers(responseEntity.getHeaders())
                .body(responseEntity.getBody());
    }*/

    @Operation(summary = "重定向")
    @GetMapping({"emby/videos/{id}/stream*", "emby/videos/{id}/original*"})
    public ResponseEntity<Void> videoRedirect(HttpServletRequest request, VideoRedirectReq req) {
        log.info("请求封装入参: {}", req);
        // 获取所有请求参数
        StringBuilder response = new StringBuilder("Received parameters:\n");
        request.getParameterMap().forEach((key, value) -> {
            response.append("Key: ").append(key).append(", Value: ").append(ArrayUtil.toString(value)).append("\n");
        });

        // 获取所有请求头
        response.append("\nReceived headers:\n");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            response.append("Header: ").append(headerName).append(", Value: ").append(headerValue).append("\n");
        }
        log.info("入参: {}", response.toString());
        String redirectUrl = embyService.videoRedirect(EmbyUtil.parseHead(request), req);
        if (StrUtil.isBlank(redirectUrl)) {
            log.info("无法重定向！");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", redirectUrl);
        log.info("重定向为: {}", redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

}