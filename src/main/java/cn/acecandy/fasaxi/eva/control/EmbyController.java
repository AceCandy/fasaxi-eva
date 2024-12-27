package cn.acecandy.fasaxi.eva.control;

import cn.acecandy.fasaxi.eva.bean.req.VideoRedirectReq;
import cn.acecandy.fasaxi.eva.service.EmbyService;
import cn.acecandy.fasaxi.eva.utils.EmbyUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Enumeration;

@Slf4j
@Tag(name = "Emby处理")
// @RestController
// @RequestMapping("/")
public class EmbyController {

    @Resource
    private EmbyService embyService;

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