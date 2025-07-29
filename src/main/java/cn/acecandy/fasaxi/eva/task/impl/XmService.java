package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.entity.XInvite;
import cn.acecandy.fasaxi.eva.dao.entity.XRenew;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserLogDao;
import cn.acecandy.fasaxi.eva.dao.service.XInviteDao;
import cn.acecandy.fasaxi.eva.dao.service.XRenewDao;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.*;
import static cn.hutool.core.util.DesensitizedUtil.DesensitizedType.ADDRESS;

/**
 * 续命码相关 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class XmService {

    @Resource
    private TgService tgService;
    @Resource
    private EmbyDao embyDao;
    @Resource
    private XRenewDao xRenewDao;
    @Resource
    private XInviteDao xInviteDao;
    @Resource
    private WodiUserLogDao wodiUserLogDao;

    public final static String 使用续命码 = "/xm_use";
    public final static String 生成续命码 = "/xm_create";

    public static final String CODE_FORMAT = "WorldLine-Mail_";
    public static final String CODE_FORMAT_OLD = "WorldLine-XRenew_";

    @Transactional(rollbackFor = Exception.class)
    public void process(String cmd, Message message) {
        switch (cmd) {
            case 生成续命码 -> createXRenew(message);
            case 使用续命码 -> useXRewnew(message);
            default -> {
            }
        }
    }

    /**
     * 生成
     *
     * @param message 消息
     */
    private void createXRenew(Message message) {
        String chatId = message.getChatId().toString();
        if (TgUtil.isGroupMsg(message)) {
            tgService.sendMsg(chatId, TIP_IN_PRIVATE, 5 * 1000);
            return;
        }
        Long userId = message.getFrom().getId();
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            tgService.sendMsg(userId.toString(), TIP_IN_OWNER, 5 * 1000);
            return;
        }
        String text = message.getText();
        int iv = 200;
        int cnt = 10;
        if (StrUtil.isNotBlank(text)) {
            String cntStr = StrUtil.trim(StrUtil.removePrefix(text, 生成续命码));
            List<String> cntList = StrUtil.splitTrim(cntStr, ' ');

            if (CollUtil.size(cntList) == 2 &&
                    NumberUtil.isNumber(CollUtil.getFirst(cntList)) && NumberUtil.isNumber(CollUtil.getLast(cntList))) {
                iv = NumberUtil.parseInt(CollUtil.getFirst(cntList));
                cnt = NumberUtil.parseInt(CollUtil.getLast(cntList));
            } else {
                tgService.sendMsg(userId.toString(), TIP_IN_XRENEW_CREATE, 5 * 1000);
                return;
            }
        }
        List<String> codes = IntStream.range(0, cnt)
                .mapToObj(i -> CODE_FORMAT + IdUtil.fastSimpleUUID())
                .toList();
        if (!xRenewDao.insertXRenw(iv, codes)) {
            tgService.sendMsg(userId.toString(), XRENEW_CREATE_ERROR, 10 * 1000);
            return;

        }
        tgService.sendMsg(userId.toString(), StrUtil.format(XRENEW_CREATE_SUCC, cnt, iv));
    }

    @SneakyThrows
    private void useXRewnew(Message message) {
        tgService.delMsg(message);
        String chatId = message.getChatId().toString();
        if (TgUtil.isGroupMsg(message)) {
            tgService.sendMsg(chatId, TIP_IN_PRIVATE, 5 * 1000);
            return;
        }
        Long userId = message.getFrom().getId();
        Emby embyUser = embyDao.findByTgId(userId);
        if (embyUser == null) {
            tgService.sendMsg(chatId, "您还未在bot处登记哦~", 5 * 1000);
            return;
        }

        String text = message.getText();
        String code = StrUtil.trim(StrUtil.removePrefix(text, 使用续命码));
        if (StrUtil.isBlank(code) || !StrUtil.startWithAny(code, CODE_FORMAT, CODE_FORMAT_OLD)) {
            tgService.sendMsg(userId.toString(), TIP_IN_XRENEW_USE, 5 * 1000);
            return;
        }
        XRenew xRenew = xRenewDao.findByCode(code);
        if (null == xRenew) {
            tgService.sendMsg(userId.toString(), TIP_IN_XRENEW_USED, 5 * 1000);
            return;
        }
        xRenew.setIsUse(true);
        xRenew.setUseTime(LocalDateTime.now());
        xRenew.setTgId(userId);
        if (!xRenewDao.update(xRenew)) {
            tgService.sendMsg(userId.toString(), XRENEW_USE_ERROR, 5 * 1000);
            return;
        }
        embyDao.upIv(userId, xRenew.getIv());
        tgService.sendMsg(userId.toString(), StrUtil.format(XRENEW_USE_SUCC, xRenew.getCode(), xRenew.getIv()));
        tgService.sendMsg(tgService.getGroup(), StrUtil.format(XRENEW_USE_GROUP_SUCC,
                TgUtil.tgName(message.getFrom()),
                StrUtil.desensitized(xRenew.getCode(), ADDRESS), xRenew.getIv()));
        ;
        log.warn("[续命码使用] 用户: {}, 续命码: {}, 奖励: {}", TgUtil.tgName(message.getFrom()), xRenew.getCode(), xRenew.getIv());
    }

    @SneakyThrows
    public void bleedBuff() {
        List<Emby> embyList = embyDao.findOnHasAccount();
        if (CollUtil.isEmpty(embyList)) {
            return;
        }
        List<Long> tgIds = embyList.stream().map(Emby::getTg).toList();
        // 剔除含有门人的tgId
        List<XInvite> xInvites = xInviteDao.findInviteeByInviter(tgIds);
        Set<Long> inviteeTgIds = xInvites.stream().filter(x ->
                        DateUtil.betweenDay(DateUtil.date(), x.getJoinTime(), false) < 22)
                .map(XInvite::getInviterId).collect(Collectors.toSet());

        // 剔除7天内参加过游戏的tgId
        Set<Long> activeTgIds = wodiUserLogDao.findOnTgIdSevenDay();
        activeTgIds.addAll(inviteeTgIds);

        // 找到不活跃账号
        List<Long> inactiveTgIds = CollUtil.subtractToList(tgIds, activeTgIds);
        List<Emby> inactiveEmbyList = embyList.stream()
                .filter(emby -> CollUtil.contains(inactiveTgIds, emby.getTg()))
                .toList();
        inactiveEmbyList.forEach(emby -> {
            int iv = RandomUtil.randomInt(2, 5);
            if (emby.getIv() >= 500) {
                iv += RandomUtil.randomInt(1, 6);
            }
            if (emby.getIv() >= 1500) {
                iv += RandomUtil.randomInt(1, 6);
            }
            if (emby.getIv() >= 4000) {
                iv += RandomUtil.randomInt(1, 6);
            }
            embyDao.upIv(emby.getTg(), -iv);
            log.warn("[🩸] 用户: {}, 减少: {}", emby.getTg(), -iv);
        });
    }

    public static void main(String[] args) {
        Console.log(StrUtil.desensitized("WorldLine-XRenew_490f408c2fb94e59b87d92e8def5f352", ADDRESS));
    }
}