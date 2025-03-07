package cn.acecandy.fasaxi.eva.task;

import cn.acecandy.fasaxi.eva.task.impl.CommonGameService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 通用定时任务
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class CommonTask {

    @Resource
    private CommonGameService service;

    /**
     * 看图猜成语
     * exec ktccy
     */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void execKtccy() {
        try {
            service.execKtccy();
        } catch (Exception e) {
            log.error("执行异常-看图猜成语 ", e);
        }
    }

    /**
     * 看图猜番号
     * exec ktccy
     */
    @Scheduled(cron = "0 13 4,16 * * ?")
    public void execKtcfh() {
        try {
            service.execKtcfh();
        } catch (Exception e) {
            log.error("执行异常-看图猜番号 ", e);
        }
    }
}