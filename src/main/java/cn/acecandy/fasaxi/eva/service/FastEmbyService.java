package cn.acecandy.fasaxi.eva.service;

import cn.acecandy.fasaxi.eva.bot.impl.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.config.FastEmbyConfig;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
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

    public ResponseEntity<?> handleEmbyRequest(HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        log.info("收到请求: [{}]{}", request.getMethod(), requestUrl);
        // String requestPath = "emby/" + CollUtil.get(StrUtil.split(requestUrl, "/emby/"), 1);
        String requestPath = StrUtil.removePrefix(requestUrl, "/");
        String originalUrl = fastEmbyConfig.getHost() + "/" + requestPath;
        String ua = request.getHeader("User-Agent");
        Map<String, String[]> paramsDict = request.getParameterMap();
        log.info("UA:{} 提取的参数: {}", ua, paramsDict);

        String apiKey = request.getParameter("api_key");
        String mediaSourceId = request.getParameter("MediaSourceId");
        if (StrUtil.isBlank(apiKey) || StrUtil.containsIgnoreCase(requestUrl, "/subtitles/")) {
            log.info("非播放请求或字幕请求,使用原始URL: {}", originalUrl);
            return proxyRequest(originalUrl, request);
        }
        /*String videoId = CollUtil.getFirst(StrUtil.split(path, "/"));
        if (mediaSourceId == null) {
            mediaSourceId = videoId;
        }*/
        mediaSourceId = StrUtil.replace(mediaSourceId, "mediasource_", "");
        if (!StrUtil.containsIgnoreCase(requestPath.toLowerCase(), "videos/")) {
            log.info("这不是播放请求,使用原始URL: {}", originalUrl);
            return proxyRequest(originalUrl, request);
        }
        String redirectUrl = StrUtil.format("{}/Items?Ids={}&Fields=Path,MediaSources&Limit=1&api_key={}",
                fastEmbyConfig.getHost(), mediaSourceId, apiKey);
        // HttpHeaders headers = new HttpHeaders();
        // headers.add("Location", redirectUrl);
        // log.info("重定向为: {}", redirectUrl);
        // return new ResponseEntity<>(headers, HttpStatus.FOUND);
        return redirect(redirectUrl, ua, originalUrl, request, requestPath);
    }

    public ResponseEntity<?> redirect(String url, String ua, String originalUrl,
                                      HttpServletRequest request, String requestPath) {
        try (HttpResponse res = HttpRequest.get(url).timeout(2000).executeAsync()) {
            if (!res.isOk() || StrUtil.isBlank(res.body())) {
                log.info("获取MediaSources失败,使用原始URL: {}", originalUrl);
                return proxyRequest(originalUrl, request);
            }
            JSONObject resJn = JSONObject.parseObject(res.body());
            String filePath = resJn.getJSONArray("Items").getJSONObject(0)
                    .getJSONArray("MediaSources").getJSONObject(0).getString("Path");
            if (StrUtil.isBlank(filePath)) {
                log.info("获取MediaSources失败,使用原始URL: {}", originalUrl);
                return proxyRequest(originalUrl, request);
            }
            String originalPath = filePath;
            filePath = URLUtil.decode(filePath);
            log.info("Emby源文件: {}", filePath);

            if (StrUtil.contains(filePath, "/udp/")) {
                originalUrl = StrUtil.format("{}/{}", fastEmbyConfig.getHost(), requestPath);
                log.info("这是IPTV请求,使用原始URL: {}", originalUrl);
                return redirect302(originalUrl);
            }

            if (!isStrmFilePath(filePath)) {
                log.info("命中本地文件路径,使用原始URL: {}", originalUrl);
                return proxyRequest(originalUrl, request);
            }
            String requestUrl = replaceFilePathMapping(filePath);
            // 这块延迟看起来无用
            /*if (requestLimit && infuseRequestLimit > 0) {
                try {
                    double delay = 1.0 / infuseRequestLimit;
                    logger.info("这是Infuse播放请求,延迟 " + delay + " 秒");
                    Thread.sleep((long) (delay * 1000));
                } catch (ArithmeticException e) {
                    logger.warning("infuseRequestLimit 配置无效，跳过延迟");
                }
            }*/
            HttpRequest headRequest = HttpUtil.createRequest(Method.HEAD, requestUrl)
                    .header("User-Agent", ua);
            try (HttpResponse headResponse = headRequest.execute()) {
                String redirectUrl = headResponse.header("Location");

                if (StrUtil.isBlank(redirectUrl) || StrUtil.equalsIgnoreCase(redirectUrl, requestUrl)) {
                    log.info("请求302 URL失败，请检查路径替换是否成功，以及cookies是否失效: {}", requestUrl);
                    return proxyRequest(originalUrl, request);
                } else {
                    return redirect302(redirectUrl);
                }
            }
        }
    }

    private static ResponseEntity<?> redirect302(String redirectUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", redirectUrl);
        log.info("重定向到: {}", redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * //TODO 获取重定向url
     *
     * @param filepath 文件路径
     * @return {@link String }
     */
    public static String replaceFilePathMapping(String filepath) {
        /*List<String> filePathMapping = CollUtil.newArrayList();
        String oldPath = filepath;
        for (String item : filePathMapping) {
            try {
                String[] parts = item.split(" => ");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid mapping format");
                }
                String pattern = parts[0];
                String replacement = parts[1].equals("\"\"") ? "" : parts[1];
                filepath = filepath.replace(pattern, replacement);
            } catch (Exception e) {
                logger.severe("路径替换失败,当前路径规则为 ::: " + item + " => " + e);
            }
        }
        filepath = filepath.replace("\\", "/");
        logger.info("源路径已替换: " + oldPath + " => " + filepath);*/
        return filepath;
    }

    private boolean isStrmFilePath(String filePath) {
        List<String> embyStrmPaths = fastEmbyConfig.getStrmPaths();
        if (CollUtil.isEmpty(embyStrmPaths)) {
            return false;
        }
        for (String path : embyStrmPaths) {
            if (StrUtil.startWithIgnoreCase(filePath, path)) {
                return true;
            }
        }
        return false;
        // return StrUtil.endWithIgnoreCase(filePath, ".strm");
    }

    private ResponseEntity<?> proxyRequest(String originalUrl, HttpServletRequest request) {
        try {
            HttpRequest proxyRequest = HttpUtil
                    .createRequest(Method.valueOf(request.getMethod()), originalUrl).timeout(2000);
            proxyRequest.header(rebuildHeader(request));
            try (HttpResponse httpResponse = proxyRequest.executeAsync()) {
                return ResponseEntity.status(httpResponse.getStatus())
                        .headers(rebuildHeader(httpResponse))
                        .body(httpResponse.body());
            }
        } catch (Exception e) {
            log.error("Proxy error: ", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Proxy error: " + e.getMessage());
        }
    }

    /**
     * 重新设置转换请求头
     * <p>
     * 忽略host参数
     *
     * @param request 请求
     * @return {@link HttpHeaders }
     */
    private static HttpHeaders rebuildHeader(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();

        Map<String, String> headerMap = MapUtil.newHashMap();
        for (String headerName : Collections.list(request.getHeaderNames())) {
            if (!"host".equalsIgnoreCase(headerName)) {
                headerMap.put(headerName, request.getHeader(headerName));
            }
        }
        headers.setAll(headerMap);
        return headers;
    }

    private static HttpHeaders rebuildHeader(HttpResponse resp) {
        HttpHeaders headers = new HttpHeaders();

        Map<String, String> headerMap = MapUtil.newHashMap();
        resp.headers().forEach((k, v) -> {
            headerMap.put(k, CollUtil.getFirst(v));
        });
        headers.setAll(headerMap);
        return headers;
    }


    public ResponseEntity<?> handleVideosRequest(String path, HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        log.info("收到请求: [{}]{}", request.getMethod(), requestUrl);

        String xEmbyAuth = request.getHeader("X-Emby-Authorization");
        // 支持 "Videos" 和 "videos"
        String requestPath = "";
        if (StrUtil.contains(requestUrl, "/Videos/")) {
            requestPath = StrUtil.format("Videos/{}",
                    CollUtil.get(StrUtil.split(requestUrl, "/Videos/"), 1));
        } else if (StrUtil.contains(requestUrl, "/videos/")) {
            requestPath = StrUtil.format("videos/{}",
                    CollUtil.get(StrUtil.split(requestUrl, "/videos/"), 1));
        }

        String originalUrl = fastEmbyConfig.getHost() + "/" + requestPath;
        if (StrUtil.contains(requestUrl, "/Subtitles/")) {
            log.info("字幕请求,使用原始URL: {}", originalUrl);
            return proxyRequest(originalUrl, request);
        }

        Map<String, String[]> paramsDict = request.getParameterMap();
        log.info("xEmbyAuth:{} 提取的参数: {}", xEmbyAuth, paramsDict);

        String apiKey = "";
        String mediaSourceId = "";
        String match = ReUtil.get("Token=\"(.*?)\"", xEmbyAuth, 1);
        if (match != null) {
            apiKey = match;
            mediaSourceId = request.getParameter("MediaSourceId");
        } else {
            apiKey = request.getParameter("api_key");
            mediaSourceId = request.getParameter("mediaSourceId");
        }

        if (StrUtil.isBlank(apiKey)) {
            log.info("非播放请求,使用原始URL: {}", originalUrl);
            return proxyRequest(originalUrl, request);
        }

        String videoId = CollUtil.getFirst(StrUtil.split(requestPath, "/"));
        if (mediaSourceId == null) {
            mediaSourceId = videoId;
        }
        mediaSourceId = StrUtil.removePrefix(mediaSourceId, "mediasource_");

        String ua = request.getHeader("User-Agent");
        String redirectUrl = StrUtil.format("{}/Items?Ids={}&Fields=Path,MediaSources&Limit=1&api_key={}",
                fastEmbyConfig.getHost(), mediaSourceId, apiKey);
        return redirect(redirectUrl, ua, originalUrl, request, requestPath);
    }

    public static void main(String[] args) {
        String baseUrl = "t.me/saturday_lite_bot?start=SaturDay.Lite-30-Register_K98{}G{}g15J";
        char secondReplacement = 's';

        for (char firstReplacement = 'a'; firstReplacement <= 'z'; firstReplacement++) {
            String generatedUrl = StrUtil.format(baseUrl, firstReplacement, secondReplacement);
            System.out.println(generatedUrl);
        }
    }
}