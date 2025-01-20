package cn.acecandy.fasaxi.eva.dao.service;

import cn.acecandy.fasaxi.eva.dao.entity.WodiUserLog;
import cn.acecandy.fasaxi.eva.dao.mapper.WodiUserLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 卧底用户日志 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class WodiUserLogDao {

    @Resource
    private WodiUserLogMapper wodiUserLogMapper;

    /**
     * 插入
     *
     * @param telegramId 电报id
     * @param fraction   分数
     * @param isVictory  就是胜利
     * @return int
     */
    public int addLog(Long telegramId, Integer fraction, Boolean isVictory) {
        WodiUserLog log = new WodiUserLog();
        log.setTelegramId(telegramId);
        log.setFraction(fraction);
        log.setIsVictory(isVictory);
        return wodiUserLogMapper.insert(log);
    }
}