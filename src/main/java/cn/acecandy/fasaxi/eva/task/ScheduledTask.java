package cn.acecandy.fasaxi.eva.task;

import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.acecandy.fasaxi.eva.bot.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.thread.ThreadUtil;
import jakarta.annotation.Resource;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务
 * <p>
 * 实现消息的状态变化/定时删除
 *
 * @author AceCandy
 * @since 2024/10/30
 */
@Slf4j
@Component
public class ScheduledTask {

    @Resource
    private EmbyTelegramBot embyTelegramBot;

    private static final Set<AutoDeleteMessage> AUTO_DEL_MSG_SET = new ConcurrentHashSet<>();

    private final ScheduledExecutorService scheduler = ThreadUtil.createScheduledExecutor(4);

    public void run() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Iterator<AutoDeleteMessage> iterator = AUTO_DEL_MSG_SET.iterator();
                while (iterator.hasNext()) {
                    AutoDeleteMessage next = iterator.next();
                    if (shouldDeleteMessage(next)) {
                        embyTelegramBot.deleteMessage(next.message);
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                log.warn("定时删除消息失败", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 是否需要删除
     *
     * @param message 消息
     * @return boolean
     */
    private static boolean shouldDeleteMessage(AutoDeleteMessage message) {
        return System.currentTimeMillis() - message.createTime > message.deleteTime ||
                (message.game != null && message.game.getStatus() != message.status);
    }

    public static void addAutoDeleteMessage(Message message, long deleteTime) {
        addAutoDeleteMessage(message, deleteTime, null, null);
    }

    public static void addAutoDeleteMessage(Message message, long deleteTime,
                                            GameStatus status, Game game) {
        AutoDeleteMessage autoDeleteMessage = new AutoDeleteMessage();
        autoDeleteMessage.createTime = System.currentTimeMillis();
        autoDeleteMessage.message = message;
        autoDeleteMessage.deleteTime = deleteTime > 0 ? deleteTime : 10 * 60 * 1000;
        autoDeleteMessage.status = status;
        autoDeleteMessage.game = game;
        AUTO_DEL_MSG_SET.add(autoDeleteMessage);
    }

    @EqualsAndHashCode
    static class AutoDeleteMessage {
        Message message;
        long deleteTime;
        long createTime;
        GameStatus status = null;
        Game game = null;
    }
}