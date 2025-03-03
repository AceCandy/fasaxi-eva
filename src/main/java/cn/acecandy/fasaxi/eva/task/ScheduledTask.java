// package cn.acecandy.fasaxi.eva.task;

/**
 * 定时任务
 * <p>
 * 实现消息的状态变化/定时删除
 *
 * @author AceCandy
 * @since 2024/10/30
 */
/*
@Slf4j
@Component
public class ScheduledTask {

    @Resource
    private EmbyTelegramBot tgBot;

    private final ScheduledExecutorService scheduler = ThreadUtil.createScheduledExecutor(4);

    public void run() {
        tgBot.setCommand();

        scheduler.scheduleAtFixedRate(() -> {
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
        }, 0, 1, TimeUnit.SECONDS);
    }
}*/