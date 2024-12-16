package cn.acecandy.fasaxi.eva.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.acecandy.fasaxi.eva.bean.vo.HeadVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class EmbyUtil {
    private EmbyUtil() {
    }

    private static final String EMBY_INNER_URL = "http://192.168.1.205:8096";
    private static final String EMBY_PUBLIC_URL = "http://emby-real.acecandy.cn:800";
    private static final String EMBY_API_KEY = "b8647127d2fa4ae6b27b6918ed8a0593";
    private static final String ALIST_ADDR = "http://192.168.1.205:5244";
    private static final String ALIST_PUBLIC_ADDR = "https://alist.acecandy.cn:880";
    private static final String ALIST_TOKEN =
            "alist-1fbc1ca5-4506-49d8-9950-53d9ab7edefaNH7xJwk7OYxNvaEP2Vd5fd9lpIBLanXcfNFZHtMvMQ73YfL4Z0ojIFwAjbFGsccd";

    private static final Map<String, String> PATH_ALIST_CONFIG = MapUtil.<String, String>builder()
            .put("Z:\\", "/")
            .put("100PB:", "/115")
            .put("https://alist.acecandy.cn:880/d/", "/")
            .put("http://192.168.1.205:5244/d", "/")
            .build();

    /**
     * 解析头
     * <p>
     * // Header: Accept, Value: *\/*
     * // Header: Connection, Value: close
     * // Header: User-Agent, Value: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0
     * // Header: X-Forwarded-Proto, Value: http
     * // Header: Sec-Fetch-Site, Value: same-origin
     * // Header: Sec-Fetch-Dest, Value: video
     * // Header: Host, Value: 192.168.31.184:8801
     * // Header: Accept-Encoding, Value: identity;q=1, *;q=0
     * // Header: DNT, Value: 1
     * // Header: Pragma, Value: no-cache
     * // Header: Range, Value: bytes=0-
     * // Header: Sec-Fetch-Mode, Value: no-cors
     * // Header: sec-ch-ua, Value: "Microsoft Edge";v="131", "Chromium";v="131", "Not_A Brand";v="24"
     * // Header: sec-ch-ua-mobile, Value: ?0
     * // Header: Cache-Control, Value: no-cache
     * // Header: sec-ch-ua-platform, Value: "macOS"
     * // Header: X-Forwarded-For, Value: 192.168.97.1
     * // Header: Accept-Language, Value: zh-CN,zh;q=0.9,en;q=0.8
     * // Header: X-Real-IP, Value: 192.168.97.1
     *
     * @param request 请求
     * @return {@link HeadVO }
     */
    public static HeadVO parseHead(HttpServletRequest request) {
        return HeadVO.builder().ua(request.getHeader("User-Agent")).build();
    }

    /**
     * 获取媒体信息
     *
     * @param ua            ua
     * @param mediaSourceId 媒体源id
     * @return {@link String }
     */
    public static String getItemInfo(String ua, String mediaSourceId) {
        String embyPath = fetchEmbyFilePath(mediaSourceId);
        String embyPathRes = replacePath2Alist(embyPath);
        if (StrUtil.isBlank(embyPathRes)) {
            return "";
        }
        var alistPath = buildAlistPath(embyPathRes);
        // 临时切换为bt 正常时可以注释掉
        alistPath = StrUtil.replaceFirst(alistPath, "micu-bt", "micu-pt");
        try (HttpResponse res = HttpRequest.head(alistPath).header("User-Agent", ua).execute()) {
            if (res.getStatus() == HttpStatus.HTTP_MOVED_TEMP) {
                return CollUtil.getFirst(res.headers().get("Location"));
            }
        }
        return "";
    }

    /**
     * 获取Emby文件实际路径
     *
     * @param mediaSourceId 媒体源id
     * @return {@link String }
     */
    public static String fetchEmbyFilePath(String mediaSourceId) {
        if (StrUtil.isBlank(mediaSourceId)) {
            return "";
        }
        try (HttpResponse res = HttpRequest.get(EMBY_PUBLIC_URL + "/Items")
                .form("Fields", "Path,MediaSources")
                .form("Ids", mediaSourceId)
                .form("Limit", 1)
                .form("api_key", EMBY_API_KEY)
                .timeout(10 * 1000)
                .execute()) {
            if (res.isOk() && JSONUtil.isTypeJSON(res.body())) {
                JSONObject resJn = JSONUtil.parseObj(res.body());
                return resJn.getJSONArray("Items").getJSONObject(0)
                        .getJSONArray("MediaSources").getJSONObject(0)
                        .getStr("Path");
            }
        }
        return "";
    }

    public static String replacePath2Alist(String inputPath) {
        for (Map.Entry<String, String> entry : PATH_ALIST_CONFIG.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (!inputPath.startsWith(k)) {
                continue;
            }
            String changePath = StrUtil.replace(inputPath, k, v);
            return StrUtil.replace(URLUtil.decode(changePath), "\\", "/");
        }
        return "";
    }

    public static String buildAlistPath(String embyUri) {
        if (StrUtil.isBlank(embyUri)) {
            return "";
        }
        return ALIST_PUBLIC_ADDR + "/d" + URLUtil.encode(embyUri);
    }
}