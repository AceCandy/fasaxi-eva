package cn.acecandy.fasaxi.eva.dao.service;

import cn.acecandy.fasaxi.eva.dao.entity.WodiUserLog;
import cn.acecandy.fasaxi.eva.dao.entity.XInvite;
import cn.acecandy.fasaxi.eva.dao.mapper.WodiUserLogMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

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
     * @return int 返回主键
     */
    public int addLog(Long telegramId, Integer fraction, Boolean isVictory) {
        WodiUserLog log = new WodiUserLog();
        log.setTelegramId(telegramId);
        log.setFraction(fraction);
        log.setIsVictory(isVictory);
        wodiUserLogMapper.insert(log);
        return log.getId();
    }

    /**
     * 按id更新iv
     *
     * @param id 本我
     * @param iv 静脉内
     */
    public void upIvById(Integer id, Integer iv) {
        if (null == id || null == iv || 0 == iv) {
            return;
        }
        LambdaUpdateWrapper<WodiUserLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WodiUserLog::getId, id);
        updateWrapper.setSql("iv = iv + " + iv);
        wodiUserLogMapper.update(null, updateWrapper);
    }

    /**
     * 按id更新iv
     *
     * @param id  本我
     * @param tiv 静脉内
     */
    public void upTopIvById(Integer id, Integer tiv) {
        if (null == id || null == tiv || 0 == tiv) {
            return;
        }
        LambdaUpdateWrapper<WodiUserLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WodiUserLog::getId, id);
        updateWrapper.setSql("tiv = tiv + " + tiv);
        wodiUserLogMapper.update(null, updateWrapper);
    }

    /**
     * 按用户当天的记录
     *
     * @param tgIds 网址
     * @return {@link XInvite }
     */
    public List<WodiUserLog> findByTgIdToday(List<Long> tgIds) {
        if (CollUtil.isEmpty(tgIds)) {
            return CollUtil.newArrayList();
        }
        LambdaQueryWrapper<WodiUserLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WodiUserLog::getTelegramId, tgIds)
                // 大于等于今天的开始时间 小于等于今天的结束时间
                .between(WodiUserLog::getCreateTime, DateUtil.beginOfDay(new Date()), DateUtil.endOfDay(new Date()))
        ;
        return wodiUserLogMapper.selectList(wrapper);
    }

    /**
     * 按用户昨天的记录
     *
     * @param tgIds 网址
     * @return {@link XInvite }
     */
    public List<WodiUserLog> findByTgIdYesterday(List<Long> tgIds) {
        if (CollUtil.isEmpty(tgIds)) {
            return CollUtil.newArrayList();
        }
        LambdaQueryWrapper<WodiUserLog> wrapper = new LambdaQueryWrapper<>();
        DateTime date = DateUtil.yesterday();
        wrapper.in(WodiUserLog::getTelegramId, tgIds)
                // 大于等于昨天的开始时间 小于等于昨天的结束时间
                .between(WodiUserLog::getCreateTime, DateUtil.beginOfDay(date), DateUtil.endOfDay(date))
        ;
        return wodiUserLogMapper.selectList(wrapper);
    }
}