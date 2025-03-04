package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.bot.game.Game;
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
     * 看图猜成语 答案
     */
    public static TimedCache<String, String> GAME_CACHE
            = CacheUtil.newTimedCache(6 * 60 * 60 * 1000);

    /**
     * 看图猜成语 发言
     *
     * @param message 消息
     */
    public static boolean ktccySpeak(Message message) {
        String text = message.getText();
        if (StrUtil.isBlank(text) || !StrUtil.startWith(text, "。")) {
            return false;
        }
        Game game = GameListUtil.getGame(message.getChatId());
        if (game != null) {
            return false;
        }
        text = StrUtil.removeAllPrefix(text, "。");
        if (StrUtil.isNotBlank(text) && StrUtil.equals(text, GAME_CACHE.get("KTCCY"))) {
            GAME_CACHE.remove("KTCCY");
            return true;
        }
        return false;
    }
}