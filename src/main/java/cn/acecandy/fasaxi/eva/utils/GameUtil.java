package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.common.dto.SmallGameDTO;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static cn.acecandy.fasaxi.eva.bot.game.Command.看图猜成语;
import static cn.acecandy.fasaxi.eva.bot.game.Command.看图猜番号;

/**
 * 通用游戏 工具类
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
public final class GameUtil {
    private GameUtil() {
    }

    /**
     * 最后发言时间
     */
    public static long endSpeakTime = System.currentTimeMillis();

    /**
     * 看图猜成语 答案
     */
    public static Queue<SmallGameDTO> GAME_CACHE = new ConcurrentLinkedQueue<>();

    /**
     * 检查答案 正确就返回对应实体 否则就返回空
     *
     * @param text 答案
     */
    public static SmallGameDTO checkAnswer(String text) {
        return GAME_CACHE.stream().filter(e -> StrUtil.equalsIgnoreCase(text, e.getAnswer()))
                .findFirst().map(e -> {
                    GAME_CACHE.remove(e);
                    return e;
                }).orElse(null);
    }

    /**
     * 获得游戏奖励币子
     *
     * @param gameType 游戏类型
     * @return int
     */
    public static int getGameRewards(String gameType) {
        return switch (gameType) {
            case 看图猜成语 -> RandomUtil.randomInt(5, 8);
            case 看图猜番号 -> RandomUtil.randomInt(3, 5);
            default -> 0;
        };
    }

}