package cn.acecandy.fasaxi.eva.utils;

import cn.hutool.cache.impl.FIFOCache;
import cn.hutool.core.util.StrUtil;

import static cn.acecandy.fasaxi.eva.bean.constants.CacheConstant.DAY_7_MS;
import static cn.acecandy.fasaxi.eva.bean.constants.CacheConstant.MEDIA_CACHE_KEY;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class CacheUtil extends cn.hutool.cache.CacheUtil {
    private CacheUtil() {
    }

    /**
     * 媒体直链 缓存
     */
    public final static FIFOCache<String, String> MEDIA_CACHE = newFIFOCache(5000, DAY_7_MS);

    private static String mediaCacheKey(String ua, String mediaSourceId) {
        return StrUtil.format(MEDIA_CACHE_KEY, mediaSourceId, ua);
    }

    public static void setMediaKey(String ua, String mediaSourceId, String value) {
        MEDIA_CACHE.put(mediaCacheKey(ua, mediaSourceId), value);
    }

    public static String getMediaKey(String ua, String mediaSourceId) {
        return MEDIA_CACHE.get(mediaCacheKey(ua, mediaSourceId));
    }

}