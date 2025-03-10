package cn.acecandy.fasaxi.eva.utils;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.StrUtil;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/**
 * 通用游戏 工具类
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
public final class CommonGameUtil {
    private CommonGameUtil() {
    }

    /**
     * 最后发言时间
     */
    public static long endSpeakTime = System.currentTimeMillis();

    /**
     * 看图猜成语 答案
     */
    public static TimedCache<String, String> GAME_CACHE
            = CacheUtil.newTimedCache(60 * 60 * 1000);

    /**
     * 通用游戏 发言
     *
     * @param message 消息
     */
    public static String commonGameSpeak(Message message) {
        String text = message.getText();
        if (StrUtil.isBlank(text) || !StrUtil.startWith(text, "。")) {
            return "";
        }
        text = StrUtil.removeAllPrefix(text, "。");
        if (StrUtil.equalsIgnoreCase(text, GAME_CACHE.get("KTCCY"))) {
            GAME_CACHE.remove("KTCCY");
            return "KTCCY";
        } else if (StrUtil.equalsIgnoreCase(text, GAME_CACHE.get("KTCFH"))) {
            GAME_CACHE.remove("KTCFH");
            return "KTCFH";
        }
        return "";
    }
}