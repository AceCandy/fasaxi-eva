package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.dao.entity.WodiTop;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUserLog;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiTopDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserLogDao;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.acecandy.fasaxi.eva.utils.WdUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.CURRENT_SEASON;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.RIGISTER_CODE1;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.RIGISTER_CODE2;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.RIGISTER_CODE3;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.SEASON_ENDS;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.USER_LEVEL_UP_FIRST;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.USER_LEVEL_UP_HIGH;
import static cn.acecandy.fasaxi.eva.utils.GlobalUtil.setSpeakCnt;

/**
 * 战力榜单 实现
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
     * 按天缓存每天前十名用户的积分
     * <p>
     * 主要是今天的缓存上就行
     */
    public static final Map<String, ArrayList<Map.Entry<Long, Integer>>> DAY_TOP10
            = MapUtil.newHashMap(4);

    /**
     * 检查更新战力排行榜单
     */
    @Transactional(rollbackFor = Exception.class)
    public void powerRankCheck() {
        ArrayList<Map.Entry<Long, Integer>> top10 = findTopByCache();
        if (CollUtil.isEmpty(top10)) {
            return;
        }
        // 获取当前需要选出的下一阶段王者
        List<WodiTop> wdTop = wodiTopDao.selectTop();
        WodiTop maxTop = wdTop.stream()
                .max(Comparator.comparingInt(WodiTop::getLevel)).orElse(null);
        Integer nextLv = maxTop == null ? 1 : maxTop.getLevel() + 1;
        int nextMinScore = WdUtil.lvToMin(nextLv);

        Map.Entry<Long, Integer> king = CollUtil.getFirst(top10);
        if (king.getValue() < nextMinScore) {
            return;
        }
        Integer upScore = WdUtil.lvToFirstUpGift(nextLv);

        String registerMsg = "";
        String registerCode = "";
        if (nextLv == 8) {
            registerMsg = StrUtil.format(USER_LEVEL_UP_HIGH, 1);
            registerCode = RIGISTER_CODE1;
        } else if (nextLv == 9) {
            registerMsg = StrUtil.format(USER_LEVEL_UP_HIGH, 1);
            registerCode = RIGISTER_CODE2;
        } else if (nextLv >= 10) {
            registerMsg = StrUtil.format(USER_LEVEL_UP_HIGH, 1);
            registerCode = RIGISTER_CODE3;
        }
        Long tgId = king.getKey();
        WodiUser wodiUser = wodiUserDao.findByTgId(tgId);

        String upFirst;
        if (nextLv < 10) {
            upFirst = StrUtil.format(USER_LEVEL_UP_FIRST, TgUtil.tgNameOnUrl(wodiUser),
                    WdUtil.lvToTitle(nextLv), upScore, registerMsg);
            upScore -= 5;
            embyDao.allUpIv(5);
        } else {
            upFirst = StrUtil.format(SEASON_ENDS, TgUtil.tgNameOnUrl(wodiUser),
                    WdUtil.lvToTitle(nextLv), upScore, registerMsg, CURRENT_SEASON);
            upScore -= 50;
            embyDao.allUpIv(50);
            // 赛季结束，设置长次数
            setSpeakCnt(1199, 1999);
        }
        embyDao.upIv(tgId, upScore);
        if (StrUtil.isNotBlank(registerCode)) {
            tgService.sendMsg(tgId.toString(),
                    StrUtil.format("您获得了{}: {}", registerMsg, registerCode));
        }

        // 写入碑文
        WodiTop wodiTop = new WodiTop();
        BeanUtil.copyProperties(wodiUser, wodiTop);
        wodiTop.setId(null);
        wodiTop.setLevel(nextLv);
        wodiTop.setUpTime(new DateTime());
        wodiTop.setSeason(CURRENT_SEASON);
        wodiTopDao.insertOrUpdate(wodiTop);

        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(tgService.getGroup()).caption(upFirst)
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/s{}/lv{}.webp", CURRENT_SEASON, nextLv)),
                        "谁是卧底个人信息"))
                .build();
        Message msg = tgService.sendPhoto(sendPhoto);
        tgService.pinMsg(msg.getChatId().toString(), msg.getMessageId());
    }

    public ArrayList<Map.Entry<Long, Integer>> findTopByCache() {
        return DAY_TOP10.computeIfAbsent(DateUtil.today(), k -> findTopByDb());
    }

    /**
     * 按db查找Top10并缓存到map
     */
    private ArrayList<Map.Entry<Long, Integer>> findTopByDb() {
        List<WodiUserLog> wdLog = wodiUserLogDao.findAllWinBySeason(null);
        if (CollUtil.isEmpty(wdLog)) {
            return null;
        }
        Map<Long, Integer> userMap = buildTop3UserMap(wdLog);
        if (MapUtil.isEmpty(userMap)) {
            return null;
        }
        // 设置获取排名前十
        Map<Long, Integer> top10 = userMap.entrySet().stream()
                .filter(entry -> entry.getValue() != 0)
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new
                ));
        if (MapUtil.isEmpty(userMap)) {
            return null;
        }
        ArrayList<Map.Entry<Long, Integer>> top10List = CollUtil.newArrayList(top10.entrySet());
        DAY_TOP10.put(DateUtil.today(), top10List);
        return top10List;
    }

    /**
     * 构建每日用户top3积分结果
     *
     * @param wdLog wd日志
     * @return {@link Map }<{@link Long }, {@link Integer }>
     */
    private static Map<Long, Integer> buildTop3UserMap(List<WodiUserLog> wdLog) {
        // 使用嵌套Map替代字符串拼接键，提升哈希性能
        Map<Long, Map<String, Integer>> userDateCountMap = new HashMap<>(256);
        Map<Long, Integer> userMap = new HashMap<>(256);

        for (WodiUserLog log : wdLog) {
            Long userId = log.getTelegramId();
            String date = DateUtil.formatDate(log.getCreateTime());

            // 1. 获取用户日期计数Map，避免字符串拼接
            Map<String, Integer> dateCountMap =
                    userDateCountMap.computeIfAbsent(userId, k -> MapUtil.newHashMap());

            // 2. 原子操作计算计数，避免重复哈希查找
            int count = dateCountMap.merge(date, 1, Integer::sum);
            if (count > 3) continue;

            // 3. 使用merge替代getOrDefault+put，减少哈希查找次数
            userMap.merge(userId, log.getFraction(), Integer::sum);
        }
        return userMap;
    }

    public static void main(String[] args) {
        String userLog = "[" +
                "  {telegramId: 400, createTime: \"2023-10-03 14:00\", fraction: 10},\n" +
                "  {telegramId: 300, createTime: \"2023-10-03 14:00\", fraction: 30},\n" +
                "  {telegramId: 200, createTime: \"2023-10-03 14:00\", fraction: 30},\n" +
                "  {telegramId: 100, createTime: \"2023-10-01 09:00\", fraction: 10},\n" +
                "  {telegramId: 100, createTime: \"2023-10-01 10:00\", fraction: 20},\n" +
                "  {telegramId: 100, createTime: \"2023-10-01 11:10\", fraction: 30},\n" +
                "  {telegramId: 100, createTime: \"2023-10-01 12:02\", fraction: 40},\n" +
                "  {telegramId: 100, createTime: \"2023-10-01 12:30\", fraction: 50},\n" +
                "  {telegramId: 100, createTime: \"2023-10-02 14:00\", fraction: 30},\n" +
                "  {telegramId: 100, createTime: \"2023-10-02 14:00\", fraction: 50},\n" +
                "  {telegramId: 100, createTime: \"2023-10-03 14:00\", fraction: 50},\n" +
                "  {telegramId: 200, createTime: \"2023-10-03 14:00\", fraction: 30},\n" +
                "  {telegramId: 200, createTime: \"2023-10-02 08:00\", fraction: 50}\n" +
                "]";
        Map<Long, Integer> userMap = buildTop3UserMap(JSONUtil.toList(userLog, WodiUserLog.class));
        Console.log(userMap);
        Console.log(MapUtil.sortByValue(userMap, true));
        Map<Long, Integer> top10 = userMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        Console.log(top10);
    }
}