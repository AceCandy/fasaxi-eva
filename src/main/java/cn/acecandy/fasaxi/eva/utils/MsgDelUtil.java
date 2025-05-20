package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.StrUtil;
import lombok.EqualsAndHashCode;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import static cn.acecandy.fasaxi.eva.utils.CommonGameUtil.GAME_CACHE;

/**
 * 删除消息工具类
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
public final class MsgDelUtil {
    private MsgDelUtil() {
    }

    public static final Set<AutoDelMsg> AUTO_DEL_MSG_SET = new ConcurrentHashSet<>();
    public static Iterator<AutoDelMsg> getAutoDelMsgSet() {
        return AUTO_DEL_MSG_SET.iterator();
    }

    /**
     * 是否需要删除
     *
     * @param message 消息
     * @return boolean
     */
    public static boolean shouldDelMsg(AutoDelMsg message) {
        if (StrUtil.isNotBlank(message.commonGame)) {
            if (TIME_STRATEGY.test(message)) {
                GAME_CACHE.removeIf(smallGame ->
                        StrUtil.equalsIgnoreCase(smallGame.getType(), message.commonGame));
                return true;
            } else {
                return false;
            }
        }
        // 组合策略判断
        return TIME_STRATEGY.test(message) || STATUS_STRATEGY.test(message);
    }

    public static void addAutoDelMsg(Message message, long deleteTime) {
        addAutoDelMsg(message, deleteTime, null, null);
    }

    public static void addAutoDelMsg(Message message, long deleteTime, String commonGameType) {
        addAutoDelMsg(message, deleteTime, null, null, commonGameType);
    }

    public static void addAutoDelMsg(Message message, GameStatus status, Game game) {
        addAutoDelMsg(message, 0, status, game);
    }

    public static void addAutoDelMsg(Message message, long deleteTime,
                                     GameStatus status, Game game) {
        addAutoDelMsg(message, deleteTime, status, game, null);
    }

    public static void addAutoDelMsg(Message message, long deleteTime,
                                     GameStatus status, Game game, String commonGameType) {
        AutoDelMsg autoDeleteMessage = new AutoDelMsg();
        autoDeleteMessage.createTime = System.currentTimeMillis();
        autoDeleteMessage.message = message;
        autoDeleteMessage.deleteTime = deleteTime > 0 ? deleteTime : 10 * 60 * 1000;
        autoDeleteMessage.status = status;
        autoDeleteMessage.game = game;
        autoDeleteMessage.commonGame = commonGameType;
        AUTO_DEL_MSG_SET.add(autoDeleteMessage);
    }

    @EqualsAndHashCode
    public static class AutoDelMsg {
        public Message message;
        long deleteTime;
        long createTime;
        GameStatus status = null;
        Game game = null;
        String commonGame = null;
    }

    /**
     * 策略接口
     *
     * @author AceCandy
     * @since 2025/05/19
     */
    public interface DelStrategy extends Predicate<AutoDelMsg> {
    }

    /**
     * 时间策略
     */
    public static final DelStrategy TIME_STRATEGY = msg -> {
        long currentTime = System.currentTimeMillis();
        return currentTime - msg.createTime > msg.deleteTime;
    };

    /**
     * 状态策略
     */
    public static final DelStrategy STATUS_STRATEGY = msg ->
            msg.game != null && msg.game.getStatus() != msg.status;
}