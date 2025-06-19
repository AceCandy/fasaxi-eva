package cn.acecandy.fasaxi.eva.task;

import cn.acecandy.fasaxi.eva.task.impl.PowerRankService;
import cn.acecandy.fasaxi.eva.task.impl.XmService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 战力排名榜刷新 定时任务
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class PowerRankTask {

    @Resource
    private PowerRankService powerRankService;

    @Resource
    private XmService xmService;

    @Scheduled(cron = "0 59 7 * * ?")
    public void powerRankCheck() {
        try {
            powerRankService.powerRankCheck();
        } catch (Exception e) {
            log.error("执行异常-战力统计 ", e);
        }
        try {
            xmService.bleedBuff();
        } catch (Exception e) {
            log.error("执行异常-🩸buff ", e);
        }
    }
}