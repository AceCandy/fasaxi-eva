package cn.acecandy.fasaxi.eva.task;

import cn.acecandy.fasaxi.eva.bot.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.utils.MsgDelUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class MsgDelTask {

    @Resource
    private EmbyTelegramBot tgBot;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void checkAndDeleteMessages() {
        try {
            var iterator = MsgDelUtil.getAutoDelMsgSet();
            while (iterator.hasNext()) {
                var next = iterator.next();
                if (MsgDelUtil.shouldDeleteMessage(next)) {
                    tgBot.deleteMessage(next.message);
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            log.warn("定时删除消息失败", e);
        }
    }
}