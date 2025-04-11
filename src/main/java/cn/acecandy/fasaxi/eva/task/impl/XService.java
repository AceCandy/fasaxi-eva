package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.bot.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.entity.XInvite;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.XInviteDao;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.GENERATE_INVITE;
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
    private EmbyTelegramBot tgBot;

    @Resource
    private EmbyDao embyDao;

    @Resource
    private XInviteDao xInviteDao;

    /**
     * 看图猜成语
     */
    @Transactional(rollbackFor = Exception.class)
    public void xInvite(Message message) {
        boolean isGroupMessage = message.isGroupMessage() || message.isSuperGroupMessage();
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        if (isGroupMessage) {
            tgBot.sendMessage(chatId, TIP_IN_PRIVATE, 10 * 1000);
            return;
        }
        Emby emby = embyDao.findByTgId(userId);
        if (null == emby) {
            return;
        }
        XInvite xInvite = xInviteDao.findByInviterLast(userId);
        if (null != xInvite && DateUtil.compare(DateUtil.date(), DateUtil.endOfDay(xInvite.getCreateTime())) < 0) {
            tgBot.sendMessage(chatId, "您今天已经创建过邀请链接了", 5 * 1000);
            return;
        }

        Integer costIv = 100;
        if (emby.getIv() < costIv) {
            tgBot.sendMessage(chatId, "您的Dmail不足，无法创建邀请", 5 * 1000);
            return;
        }
        if (!CollUtil.contains(tgBot.getAdmins(), userId)) {
            embyDao.upIv(userId, -costIv);
        }
        tgBot.sendMessage(chatId, TIP_IN_INVITE, 2 * 1000);

        String inviteUrl = tgBot.generateInvite(userId, 1);
        log.info("用户:{},生成了一个邀请链接:{}", TgUtil.tgName(message.getFrom()), inviteUrl);
        tgBot.sendMessage(message.getMessageId(), message.getChatId(), StrUtil.format(GENERATE_INVITE, inviteUrl));
        xInviteDao.insertInviter(userId, inviteUrl);
    }

    public static void main(String[] args) {
        DateTime d = DateUtil.parse("2025-04-10 03:35:16");
        Console.log(DateUtil.endOfDay(d));
        Console.log(DateUtil.compare(DateUtil.date(), DateUtil.endOfDay(d)) > 0);
    }
}