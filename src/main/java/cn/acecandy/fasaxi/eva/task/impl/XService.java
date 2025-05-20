package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUserLog;
import cn.acecandy.fasaxi.eva.dao.entity.XInvite;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserLogDao;
import cn.acecandy.fasaxi.eva.dao.service.XInviteDao;
import cn.acecandy.fasaxi.eva.utils.GameUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.GENERATE_INVITE;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.INVITE_COLLECT;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TIP_IN_GROUP;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TIP_IN_INVITE;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TIP_IN_PRIVATE;

/**
 * x功能 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class XService {

    @Resource
    private TgService tgService;

    @Resource
    private EmbyDao embyDao;

    @Resource
    private XInviteDao xInviteDao;

    @Resource
    private WodiUserDao wodiUserDao;

    @Resource
    private WodiUserLogDao wodiUserLogDao;

    /**
     * 传承邀请
     */
    @Transactional(rollbackFor = Exception.class)
    public void xInvite(Message message) {
        String chatId = message.getChatId().toString();
        Long userId = message.getFrom().getId();
        if (TgUtil.isGroupMsg(message)) {
            tgService.sendMsg(chatId, TIP_IN_PRIVATE, 10 * 1000);
            return;
        }
        Emby emby = embyDao.findByTgId(userId);
        if (null == emby) {
            return;
        }
        XInvite shifu = xInviteDao.findByInvitee(userId);
        if (null != shifu && DateUtil.betweenDay(DateUtil.date(), shifu.getJoinTime(), false) < 21) {
            tgService.sendMsg(chatId, "您还未出师，无法开启传承！", 5 * 1000);
            return;
        }

        WodiUser wodiUser = wodiUserDao.findByTgId(userId);
        Integer lv = GameUtil.scoreToLv(wodiUser.getFraction());
        Integer canInviteCnt = lv / 3 + 1;
        Long cnt = xInviteDao.cntByInviterToday(userId);
        if (cnt >= canInviteCnt) {
            tgService.sendMsg(chatId,
                    "您今日创建传承邀请超限(" + canInviteCnt + ")了，请明日再来", 5 * 1000);
            return;
        }

        Integer costIv = 200;
        if (emby.getIv() < costIv) {
            tgService.sendMsg(chatId, "您的Dmail不足，无法创建传承邀请", 5 * 1000);
            return;
        }
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            embyDao.upIv(userId, -costIv);
        }
        tgService.sendMsg(chatId, TIP_IN_INVITE, 2 * 1000);

        String inviteUrl = tgService.generateInvite(userId, 1);
        log.info("{} 生成了一个传承邀请:{}", TgUtil.tgName(message.getFrom()), inviteUrl);
        tgService.sendMsg(message.getMessageId(), message.getChatId().toString(), StrUtil.format(GENERATE_INVITE, inviteUrl));
        xInviteDao.insertInviter(userId, inviteUrl);
    }

    /**
     * 传承邀请名单
     */
    @Transactional(rollbackFor = Exception.class)
    public void xInviteList(Message message) {
        String chatId = message.getChatId().toString();
        Long userId = message.getFrom().getId();
        WodiUser wodiUser = wodiUserDao.findByTgId(userId);
        Emby emby = embyDao.findByTgId(userId);
        if (null == wodiUser || null == emby) {
            tgService.sendMsg(chatId, "您还未参与过游戏或者未在助手处登记哦~", 5 * 1000);
            return;
        }
        if (TgUtil.isPrivateMsg(message)) {
            tgService.sendMsg(chatId, TIP_IN_GROUP, 10 * 1000);
            return;
        }
        // 查询弟子名单 小于21天为未出师弟子（计算22天是为了取昨日）
        List<XInvite> xInvites = xInviteDao.findInviteeByInviter(userId);
        xInvites = xInvites.stream().filter(x ->
                DateUtil.betweenDay(DateUtil.date(), x.getJoinTime(), false) < 22).toList();
        // 获取昨日弟子表现
        List<Long> yesInviteeIds = xInvites.stream().filter(x -> x.getCollectTime() == null
                        || DateUtil.compare(x.getCollectTime(), DateUtil.yesterday()) < 0)
                .map(XInvite::getInviteeId).toList();
        Map<Long, Integer> ivMap = wodiUserLogDao.findByTgIdYesterday(yesInviteeIds).stream()
                .collect(Collectors.groupingBy(WodiUserLog::getTelegramId,
                        Collectors.summingInt(bean ->
                                (int) (bean.getFraction() * 0.5 + bean.getTiv() * 0.2))
                ));
        // 发放昨日奖励
        if (MapUtil.isNotEmpty(ivMap)) {
            xInviteDao.updateCollectTime(yesInviteeIds, DateUtil.yesterday());
            int ivTotal = ivMap.values().stream().mapToInt(v -> v).sum();
            if (ivTotal > 0) {
                // 扣除2分领取
                embyDao.upIv(userId, ivTotal - 2);
                tgService.sendMsg(userId.toString(), StrUtil.format(INVITE_COLLECT, ivTotal));
            }
        }

        // 今日累计展示
        List<Long> inviteeIds = xInvites.stream().map(XInvite::getInviteeId).toList();
        Map<Long, WodiUser> embyMap = wodiUserDao.findByTgId(inviteeIds).stream().collect(
                Collectors.toMap(WodiUser::getTelegramId, v -> v, (k1, k2) -> k2));
        Map<Long, Integer> newIvMap = wodiUserLogDao.findByTgIdToday(inviteeIds).stream()
                .collect(Collectors.groupingBy(WodiUserLog::getTelegramId,
                        Collectors.summingInt(bean ->
                                (int) (bean.getFraction() * 0.5 + bean.getTiv() * 0.2))
                ));
        SendMessage sendMsg = SendMessage.builder()
                .chatId(chatId).text(GameUtil.getInviteList(wodiUser, xInvites, embyMap, newIvMap))
                .build();
        tgService.sendMsg(sendMsg, 300 * 1000);
    }

    public static void main(String[] args) {
        DateTime d = DateUtil.parse("2025-04-20 03:35:16");
        Console.log(DateUtil.betweenDay(DateUtil.date(), d, false));
    }
}