package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.StrUtil;
import lombok.EqualsAndHashCode;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Iterator;
import java.util.Set;

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

    private static final Set<AutoDeleteMessage> AUTO_DEL_MSG_SET = new ConcurrentHashSet<>();


    public static Iterator<AutoDeleteMessage> getAutoDelMsgSet() {
        return AUTO_DEL_MSG_SET.iterator();
    }

    /**
     * 是否需要删除
     *
     * @param message 消息
     * @return boolean
     */
    public static boolean shouldDeleteMessage(AutoDeleteMessage message) {
        if (StrUtil.isNotBlank(message.commonGame)) {
            if (System.currentTimeMillis() - message.createTime > message.deleteTime) {
                GAME_CACHE.removeIf(smallGame ->
                        StrUtil.equalsIgnoreCase(smallGame.getType(), message.commonGame));
                return true;
            } else {
                return false;
            }
        }
        return System.currentTimeMillis() - message.createTime > message.deleteTime ||
                (message.game != null && message.game.getStatus() != message.status);
    }

    public static void addAutoDeleteMessage(Message message, long deleteTime) {
        addAutoDeleteMessage(message, deleteTime, null, null);
    }

    public static void addAutoDeleteMessage(Message message, long deleteTime, String commonGameType) {
        addAutoDeleteMessage(message, deleteTime, null, null, commonGameType);
    }

    public static void addAutoDeleteMessage(Message message, GameStatus status, Game game) {
        addAutoDeleteMessage(message, 0, status, game);
    }

    public static void addAutoDeleteMessage(Message message, long deleteTime,
                                            GameStatus status, Game game) {
        addAutoDeleteMessage(message, deleteTime, status, game, null);
    }

    public static void addAutoDeleteMessage(Message message, long deleteTime,
                                            GameStatus status, Game game, String commonGameType) {
        AutoDeleteMessage autoDeleteMessage = new AutoDeleteMessage();
        autoDeleteMessage.createTime = System.currentTimeMillis();
        autoDeleteMessage.message = message;
        autoDeleteMessage.deleteTime = deleteTime > 0 ? deleteTime : 10 * 60 * 1000;
        autoDeleteMessage.status = status;
        autoDeleteMessage.game = game;
        autoDeleteMessage.commonGame = commonGameType;
        AUTO_DEL_MSG_SET.add(autoDeleteMessage);
    }

    @EqualsAndHashCode
    public static class AutoDeleteMessage {
        public Message message;
        long deleteTime;
        long createTime;
        GameStatus status = null;
        Game game = null;
        String commonGame = null;
    }
}