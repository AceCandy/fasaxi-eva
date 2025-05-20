package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.dao.entity.WodiTop;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUserLog;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiTopDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserLogDao;
import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 实力榜单 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class PowerRankService {

    @Resource
    private TgService tgService;

    @Resource
    private EmbyDao embyDao;

    @Resource
    private WodiUserDao wodiUserDao;

    @Resource
    private WodiUserLogDao wodiUserLogDao;

    @Resource
    private WodiTopDao wodiTopDao;

    /**
     * 检查更新实力排行榜单
     */
    @Transactional(rollbackFor = Exception.class)
    public void powerRankCheck(Integer season) {
        List<WodiUserLog> wdLog = wodiUserLogDao.findAllBySeason(season);
        if (CollUtil.isEmpty(wdLog)) {
            return;
        }
        List<WodiTop> wdTop = wodiTopDao.selectTop(season);
        WodiTop maxTop = wdTop.stream()
                .max(Comparator.comparingInt(WodiTop::getLevel)).orElse(null);
        Integer nextLevel = maxTop == null ? 1 : maxTop.getLevel() + 1;

        // GameUtil.levelByLv()

    }

}