package cn.acecandy.fasaxi.eva.dao.service;

import cn.acecandy.fasaxi.eva.dao.entity.WodiUserLog;
import cn.acecandy.fasaxi.eva.dao.mapper.WodiUserLogMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.CURRENT_SEASON;

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
        log.setSeason(CURRENT_SEASON);
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
     * @return {@link WodiUserLog }
     */
    public List<WodiUserLog> findByTgIdToday(List<Long> tgIds) {
        if (CollUtil.isEmpty(tgIds)) {
            return CollUtil.newArrayList();
        }
        LambdaQueryWrapper<WodiUserLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WodiUserLog::getTelegramId, tgIds)
                // 大于等于今天的开始时间 小于等于今天的结束时间
                .between(WodiUserLog::getCreateTime, DateUtil.beginOfDay(new Date()), DateUtil.endOfDay(new Date()));
        return wodiUserLogMapper.selectList(wrapper);
    }

    /**
     * 按用户昨天的记录
     *
     * @param tgIds 网址
     * @return {@link WodiUserLog }
     */
    public List<WodiUserLog> findByTgIdYesterday(List<Long> tgIds) {
        if (CollUtil.isEmpty(tgIds)) {
            return CollUtil.newArrayList();
        }
        LambdaQueryWrapper<WodiUserLog> wrapper = new LambdaQueryWrapper<>();
        DateTime date = DateUtil.yesterday();
        wrapper.in(WodiUserLog::getTelegramId, tgIds)
                // 大于等于昨天的开始时间 小于等于昨天的结束时间
                .between(WodiUserLog::getCreateTime, DateUtil.beginOfDay(date), DateUtil.endOfDay(date));
        return wodiUserLogMapper.selectList(wrapper);
    }

    /**
     * 查询七天内有活跃（三条记录）的用户
     *
     * @return {@link WodiUserLog }
     */
    public Set<Long> findOnTgIdSevenDay() {
        DateTime yesterday = DateUtil.yesterday();
        DateTime sevenDay = DateUtil.offsetDay(yesterday, -6);
        LambdaQueryWrapper<WodiUserLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(WodiUserLog::getCreateTime, DateUtil.beginOfDay(sevenDay),
                DateUtil.endOfDay(yesterday));
        // 大于等于昨天的开始时间 小于等于昨天的结束时间
        /*return wodiUserLogMapper.selectList(wrapper).stream()
                .map(WodiUserLog::getTelegramId).collect(Collectors.toSet());*/
        return wodiUserLogMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(WodiUserLog::getTelegramId, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() >= 3)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    /**
     * 按赛季查找
     * <p>
     * 时间是今天之前的所有（不包含今天）
     * 按telegram_id升序、fraction降序、create_time降序排列
     *
     * @param season 季节
     * @return {@link List }<{@link WodiUserLog }>
     */
    public List<WodiUserLog> findAllWinBySeason(Integer season) {
        if (null == season) {
            season = CURRENT_SEASON;
        }
        QueryWrapper<WodiUserLog> wrapper = new QueryWrapper<>();
        wrapper.eq("season", season)
                .eq("is_victory", true)
                .lt("create_time", LocalDate.now())
                .orderByAsc("telegram_id")
                .orderByAsc("DATE(create_time)")
                .orderByDesc("fraction");
        return wodiUserLogMapper.selectList(wrapper);
    }

    /**
     * 按赛季查找
     * <p>
     * 时间是今天之前的所有（不包含今天）
     * 按telegram_id升序、fraction降序、create_time降序排列
     *
     * @param season 季节
     * @return {@link List }<{@link WodiUserLog }>
     */
    public List<WodiUserLog> findYesterdayWinBySeason(Integer season) {
        if (null == season) {
            season = CURRENT_SEASON;
        }
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        LambdaQueryWrapper<WodiUserLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WodiUserLog::getSeason, season).eq(WodiUserLog::getIsVictory, true)
                .between(WodiUserLog::getCreateTime, yesterday, today);
        return wodiUserLogMapper.selectList(wrapper);
    }

    public static void main(String[] args) {
    }
}