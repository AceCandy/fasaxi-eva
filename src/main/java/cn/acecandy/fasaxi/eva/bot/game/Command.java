package cn.acecandy.fasaxi.eva.bot.game;

import cn.acecandy.fasaxi.eva.common.dto.RedDTO;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.task.impl.GameService;
import cn.acecandy.fasaxi.eva.task.impl.RedService;
import cn.acecandy.fasaxi.eva.task.impl.TgService;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.SB_0401_GIFT;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.SB_0401_TIP;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TIP_IN_GROUP;
import static cn.acecandy.fasaxi.eva.utils.TgUtil.SB_BOX_GIFT;
import static cn.acecandy.fasaxi.eva.utils.TgUtil.SB_BOX_REGIST_NO;
import static cn.acecandy.fasaxi.eva.utils.TgUtil.isGroupMsg;

/**
 * 命令处理类，转入命令 仅命令名 （无‘/’，无‘@****’）
 *
 * @author AceCandy
 * @since 2025/05/21
 */
@Slf4j
@Component
public class Command {

    @Resource
    private TgService tgService;
    @Resource
    private GameService gameService;
    @Resource
    private RedService redService;


    @Getter
    private volatile Message sbMsg;
    @Getter
    private volatile String sbChatId;

    private final static Map<Long, String> SB_USER_LIST = MapUtil.newConcurrentHashMap();

    private final static String 惊喜盒子 = "/wd_sb";
    public final static String 看图猜成语 = "/wd_ktccy";
    public final static String 看图猜番号 = "/wd_ktcfh";

    @Resource
    private EmbyDao embyDao;

    public void process(@NotNull String command, Message message) {
        String chatId = message.getChatId().toString();
        Long userId = message.getFrom().getId();
        Integer msgId = message.getMessageId();

        if (StrUtil.startWithIgnoreCase(command, "/wd") &&
                !isGroupMsg(message) && !CollUtil.contains(tgService.getAdmins(), userId)) {
            // tgBot.sendMessage(msgId, chatId, TIP_IN_GROUP, 10 * 1000);
            tgService.sendMsg(chatId, TIP_IN_GROUP, 10 * 1000);
            return;
        }

        switch (command) {
            case 惊喜盒子:
                handleSbCommand(tgService.getGroup(), userId,
                        StrUtil.trim(StrUtil.removePrefix(message.getText(), 惊喜盒子)));
                break;
            case 看图猜成语:
                if (isAllowCommonGameCommand(tgService.getGroup(), userId)) {
                    gameService.ktccy();
                }
                break;
            case 看图猜番号:
                if (isAllowCommonGameCommand(tgService.getGroup(), userId)) {
                    gameService.execKtcfh();
                }
                break;
            default:
                break;
        }
    }


    /**
     * 是否玩家用户
     *
     * @param chatId 聊天id
     * @param userId 用户id
     * @return boolean
     */
    public Emby isEmbyUser(String chatId, Long userId) {
        Emby embyUser = embyDao.findByTgId(userId);
        if (embyUser == null) {
            tgService.sendMsg(chatId, "您还未在bot处登记哦~", 5 * 1000);
        }
        return embyUser;
    }


    /**
     * 处理 惊喜盒子
     *
     * @param chatId 聊天id
     * @param userId 用户id
     */
    private void handleSbCommand(String chatId, Long userId, String text) {
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            tgService.sendMsg(chatId, "您无法发起活动", 5 * 1000);
            return;
        }
        SendAnimation sendAnimation = SendAnimation.builder()
                .chatId(chatId).caption(SB_0401_TIP)
                .animation(new InputFile(
                        ResourceUtil.getStream("static/pic/礼盒.gif"), "礼盒.gif"))
                .replyMarkup(TgUtil.getSbBtn(Integer.valueOf(text)))
                .build();
        sbChatId = chatId;
        sbMsg = tgService.sendAnimation(sendAnimation);
        Collections.shuffle(SB_BOX_GIFT);
    }

    /**
     * 是否允许发起小游戏
     *
     * @param chatId 聊天id
     * @param userId 用户id
     */
    private boolean isAllowCommonGameCommand(String chatId, Long userId) {
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            tgService.sendMsg(chatId, "您无法发起活动", 5 * 1000);
            return false;
        }
        return true;
    }


    /**
     * 用户领取礼盒
     *
     * @param callback 召回
     * @param user     用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleEditSb(AnswerCallbackQuery callback, User user) {
        Long userId = user.getId();
        if (sbMsg == null) {
            callback.setText("❌ 活动已结束");
            return;
        }
        int cnt = TgUtil.SB_BOX_CNT.get();
        if (cnt <= 0) {
            tgService.editMsg(sbMsg, "🎁已全部领完了哦～，再次祝大家节日快乐♪٩(´ω`)و♪，明年见！");
            return;
        }
        Emby emby = isEmbyUser(sbChatId, userId);
        if (null == emby) {
            callback.setText("❌ 未在bot处登记");
            return;
        }
        Integer costIv = 50;
        if (emby.getIv() < costIv) {
            callback.setText("❌ 您的Dmail不足，无法领取礼盒");
            return;
        }
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            if (SB_USER_LIST.containsKey(userId)) {
                callback.setText("❌ 只有一次机会哦");
                return;
            }
            embyDao.upIv(userId, -costIv);
        }
        tgService.editMsg(sbMsg, sbMsg.getCaption(), TgUtil.getSbBtn(null));
        String gift = SB_BOX_GIFT.remove(ThreadLocalRandom.current().nextInt(SB_BOX_GIFT.size()));
        String giftMsg = switch (gift) {
            case "快活的空气" -> "💰Dmail +0";
            case "司墨的微笑" -> "🤣 💰Dmail +0";
            case "倒影的凝视" -> "👀 💰Dmail +0";
            case "一半的码子" -> SB_BOX_REGIST_NO.poll();
            case "爱的续期" -> "⌛️WorldLine-30-Renew_zICTzFBZH4";
            case "四倍的幸运" -> "💰Dmail +200";
            case "三倍的祝福" -> "💰Dmail +150";
            case "双倍的回赠" -> "💰Dmail +100";
            case "等价交换的宿命" -> "💰Dmail +50";
            case "真心的一半" -> "💰Dmail +25";
            default -> gift;
        };
        int dmail = 0;
        if (StrUtil.contains(giftMsg, "Dmail")) {
            dmail = Integer.parseInt(CollUtil.getLast(StrUtil.split(giftMsg, "+")));
        }
        if (dmail != 0) {
            embyDao.upIv(userId, dmail);
        }

        SendMessage sendMessage = new SendMessage(userId.toString(), StrUtil.format(SB_0401_GIFT, gift, giftMsg));
        tgService.sendMsg(sendMessage);
        log.info("{} 在礼盒活动中获得了 {}，领取了 {}", TgUtil.tgNameOnUrl(user), gift, giftMsg);

        callback.setText("✅ 花费50Dmail成功！");
        SB_USER_LIST.put(userId, "");
        Collections.shuffle(SB_BOX_GIFT);
    }

    /**
     * 用户领取红包
     *
     * @param callback 召回
     * @param user     用户
     */
    public void handleRed(AnswerCallbackQuery callback, User user, String redId) {
        RedDTO envelope = redService.getRedEnvelope(redId);
        if (null == envelope) {
            callback.setText("❌ 红包已过期");
            return;
        }
        Message msg = envelope.getMsg();
        if (null == msg) {
            callback.setText("❌ 红包已过期");
            return;
        }
        if (envelope.isEmpty()) {
            redService.removeRedEnvelope(redId);
            tgService.editMsg(msg, envelope.getFinalMessage());
            return;
        }
        callback.setText(redService.grabRed(redId, user));
        if (StrUtil.startWith(callback.getText(), "🧧")) {
            callback.setShowAlert(true);
        }
        if (redService.getRedEnvelope(redId).isEmpty()) {
            redService.removeRedEnvelope(redId);
            tgService.editMsg(msg, envelope.getFinalMessage());
            return;
        }
    }

}