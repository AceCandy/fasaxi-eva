package cn.acecandy.fasaxi.eva.emby.wrapper;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

/**
 * emby请求体缓存包装器
 *
 * @author tangningzhu
 * @since 2025/4/15
 */
public class EmbyContentCacheReqWrapper extends HttpServletRequestWrapper {

    private byte[] cachedContent;
    private final Map<String, String> cachedHeader = MapUtil.newHashMap();
    private final Map<String, Object> cachedParam = MapUtil.newHashMap();

    public EmbyContentCacheReqWrapper(HttpServletRequest request) throws IOException {
        super(request);
        cacheHeader(request);
        cacheParam(request);
        cacheInputStream(request);
    }

    private void cacheInputStream(HttpServletRequest request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = request.getInputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }
        byte[] originalContent = baos.toByteArray();
        this.cachedContent = originalContent;

        /*String charset = request.getCharacterEncoding();
        if (charset == null) charset = StandardCharsets.UTF_8.name();
        String queryString = new String(originalContent, charset);
        queryString += "&X-Emby-Token=e2262107a13c45a7bfc48884be6f98ad";
        this.cachedContent = queryString.getBytes();*/
    }

    private void cacheHeader(HttpServletRequest request) throws IOException {
        Map<String, String> headerMap = MapUtil.newHashMap();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!StrUtil.equalsAnyIgnoreCase(headerName, "Host", "Content-Length", "Referer")) {
                String headerValue = request.getHeader(headerName);
                if (StrUtil.equalsIgnoreCase(headerName, "User-Agent")) {
                    headerValue = "Yamby/1.0";
                }
                headerMap.put(headerName, headerValue);
            }
        }
        cachedHeader.putAll(headerMap);
    }

    private void cacheParam(HttpServletRequest request) throws IOException {
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            cachedParam.put(paramName, request.getParameter(paramName));
        }
    }

    public byte[] getContentAsByteArray() {
        return this.cachedContent;
    }

    public Map<String, String> getHeaderMap() {
        return this.cachedHeader;
    }

    public Map<String, Object> getCacheParam() {
        return this.cachedParam;
    }
}