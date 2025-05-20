package cn.acecandy.fasaxi.eva.task;

import cn.acecandy.fasaxi.eva.task.impl.PowerRankService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.CURRENT_SEASON;

/**
 * 实力排名榜刷新 定时任务
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class PowerRankTask {

    @Resource
    private PowerRankService powerRankService;

    @Scheduled(cron = "0 59 7 * * ?")
    public void powerRankCheck() {
        try {
            powerRankService.powerRankCheck(CURRENT_SEASON);
        } catch (Exception e) {
            log.error("执行异常-看图猜番号 ", e);
        }
    }
}