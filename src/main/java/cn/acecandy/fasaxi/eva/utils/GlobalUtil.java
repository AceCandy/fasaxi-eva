package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.RandomUtil;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全局的一些变量
 *
 * @author tangningzhu
 * @since 2024/11/25
 */
public final class GlobalUtil {
    private GlobalUtil() {
    }

    /**
     * 全局发言数量
     */
    public static final AtomicInteger GAME_SPEAK_CNT = new AtomicInteger(RandomUtil.randomInt(1, 20));
    /**
     * 最后发言时间
     */
    public static Long lastSpeakTime = System.currentTimeMillis();

    /**
     * 更新发言
     */
    public static void updateSpeak() {
        GAME_SPEAK_CNT.decrementAndGet();
        lastSpeakTime = System.currentTimeMillis();
    }

    /**
     * 更新发言
     */
    public static void setSpeakCnt(int min, int max) {
        GAME_SPEAK_CNT.set(RandomUtil.randomInt(min, max));
    }

    /**
     * 更新发言
     */
    public static void addSpeakCnt(int min, int max) {
        GAME_SPEAK_CNT.addAndGet(RandomUtil.randomInt(min, max));
    }

    /**
     * 排名缓存
     */
    public static TimedCache<String, List<WodiUser>> RANK_CACHE
            = CacheUtil.newTimedCache(600 * 1000);
    public static volatile Message rankMsg;
}