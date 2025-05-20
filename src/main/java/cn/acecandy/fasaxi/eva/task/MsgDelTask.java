package cn.acecandy.fasaxi.eva.task;

import cn.acecandy.fasaxi.eva.task.impl.TgService;
import cn.acecandy.fasaxi.eva.utils.MsgDelUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static cn.acecandy.fasaxi.eva.utils.MsgDelUtil.AUTO_DEL_MSG_SET;

/**
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class MsgDelTask {

    @Resource
    private TgService tgService;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void checkAndDeleteMessages() {
        try {
            var iterator = MsgDelUtil.getAutoDelMsgSet();
            while (iterator.hasNext()) {
                var next = iterator.next();
                if (MsgDelUtil.shouldDelMsg(next)) {
                    try {
                        AUTO_DEL_MSG_SET.remove(next);
                        tgService.delMsg(next.message);
                    } catch (Exception e) {
                        log.warn("定时删除消息失败", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("执行异常-看图猜番号 ", e);
        }
    }
}