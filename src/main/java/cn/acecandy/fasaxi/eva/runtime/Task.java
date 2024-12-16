package cn.acecandy.fasaxi.eva.runtime;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.thread.ThreadUtil;
import cn.acecandy.fasaxi.eva.bin.GameStatus;
import cn.acecandy.fasaxi.eva.bot.impl.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.game.Game;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Set;

/**
 * 任务
 *
 * @author AceCandy
 * @since 2024/10/30
 */
@Slf4j
@Component
public class Task {

    @Resource
    private EmbyTelegramBot embyTelegramBot;

    private static final Set<AutoDeleteMessage> AUTO_DEL_MSG_SET = new ConcurrentHashSet<>();


    public void run() {
        while (true) {
            try {
                // GameList.UNSENT_USER_WORDS.removeIf(w -> !w.game.run);
                AUTO_DEL_MSG_SET.forEach(next -> {
                    if (System.currentTimeMillis() - next.createTime > next.deleteTime ||
                            (next.game != null && next.game.getStatus() != next.status)) {
                        embyTelegramBot.deleteMessage(next.message);
                        AUTO_DEL_MSG_SET.remove(next);
                        // Console.log("task执行中……自动删除信息");
                    }
                });
            } catch (Exception e) {
                log.warn("定时删除消息失败", e);
            }
            //   1秒执行一次 ，1s Run
            ThreadUtil.safeSleep(1000);
        }
    }

    public static void addAutoDeleteMessage(Message message, long deleteTime) {
        AutoDeleteMessage autoDeleteMessage = new AutoDeleteMessage();
        autoDeleteMessage.createTime = System.currentTimeMillis();
        autoDeleteMessage.message = message;
        autoDeleteMessage.deleteTime = deleteTime;
        AUTO_DEL_MSG_SET.add(autoDeleteMessage);
    }

    public static void addAutoDeleteMessage(Message message, long deleteTime,
                                            GameStatus status, Game game) {
        AutoDeleteMessage autoDeleteMessage = new AutoDeleteMessage();
        autoDeleteMessage.createTime = System.currentTimeMillis();
        autoDeleteMessage.message = message;
        autoDeleteMessage.deleteTime = deleteTime > 0 ? deleteTime : 1200000;
        autoDeleteMessage.status = status;
        autoDeleteMessage.game = game;
        AUTO_DEL_MSG_SET.add(autoDeleteMessage);

    }

    static class AutoDeleteMessage {
        Message message;
        long deleteTime;
        long createTime;
        GameStatus status = null;
        Game game = null;
    }
}