package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.common.dto.SmallGameDTO;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import org.telegram.telegrambots.meta.api.objects.message.Message;

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
    public static Queue<SmallGameDTO> GAME_CACHE = new ConcurrentLinkedQueue<>();

    /**
     * 通用游戏 发言
     *
     * @param message 消息
     */
    public static SmallGameDTO commonGameSpeak(Message message) {
        String text = message.getText();
        if (StrUtil.isBlank(text) || !StrUtil.startWith(text, "。")) {
            return null;
        }
        text = StrUtil.removeAllPrefix(text, "。");
        for (SmallGameDTO smallGame : GAME_CACHE) {
            if (StrUtil.equalsIgnoreCase(text, smallGame.getAnswer())) {
                // return GAME_CACHE.poll();
                if (GAME_CACHE.remove(smallGame)) {
                    return smallGame;
                }
            }
        }
        return null;
    }

    /**
     * 获得游戏奖励币子
     *
     * @param gameType 游戏类型
     * @return int
     */
    public static int getGameRewards(String gameType) {
        return switch (gameType) {
            case 看图猜成语 -> RandomUtil.randomInt(6, 8);
            case 看图猜番号 -> RandomUtil.randomInt(4, 5);
            default -> 0;
        };
    }

}