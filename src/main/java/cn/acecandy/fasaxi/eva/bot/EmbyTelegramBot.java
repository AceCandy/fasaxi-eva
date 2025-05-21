package cn.acecandy.fasaxi.eva.bot;

import cn.acecandy.fasaxi.eva.bot.game.Command;
import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.bot.game.GameEvent;
import cn.acecandy.fasaxi.eva.bot.game.GameUser;
import cn.acecandy.fasaxi.eva.common.dto.SmallGameDTO;
import cn.acecandy.fasaxi.eva.config.EmbyBossConfig;
import cn.acecandy.fasaxi.eva.dao.entity.XInvite;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.XInviteDao;
import cn.acecandy.fasaxi.eva.task.impl.TgService;
import cn.acecandy.fasaxi.eva.utils.CommonGameUtil;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatJoinRequest;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.COMMON_WIN;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.WARNING_EDIT;
import static cn.acecandy.fasaxi.eva.common.enums.GameStatus.讨论时间;

/**
 * emby电报机器人
 *
 * @author AceCandy
 * @since 2024/10/21
 */
@Lazy
@Slf4j
@Component
public class EmbyTelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    @Resource
    public TgService tgService;
    @Resource
    public Command command;
    @Resource
    public GameEvent gameEvent;
    @Resource
    public EmbyBossConfig embyBossConfig;
    @Resource
    public EmbyDao embyDao;
    @Resource
    public XInviteDao xInviteDao;

    @Override
    public String getBotToken() {
        return embyBossConfig.getToken();
    }


    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        handleUpdate(update);
    }

    private void handleUpdate(Update update) {
        // Console.log(JSONUtil.toJsonStr(update));
        Message msg = update.getMessage();
        Message editMsg = update.getEditedMessage();
        ChatJoinRequest joinRequest = update.getChatJoinRequest();
        CallbackQuery callback = update.getCallbackQuery();

        if (msg != null && TgUtil.isMessageValid(msg)) {
            handleMsg(msg);
        } else if (editMsg != null) {
            handleEditMsg(editMsg);
        } else if (callback != null) {
            handleCallbackQuery(update);
        } else if (joinRequest != null) {
            handleChatJoinRequest(joinRequest);
        }
    }


    /**
     * 处理传入消息
     *
     * @param message 消息
     */
    private void handleMsg(Message message) {
        boolean isCommand = message.isCommand();
        boolean isGroupMessage = TgUtil.isGroupMsg(message);
        String msgData = message.getText();

        if (isCommand) {
            String msg = TgUtil.extractCommand(msgData, tgService.getBotUsername());
            command.process(msg, message, isGroupMessage);
        } else {
            if (isGroupMessage) {
                CommonGameUtil.endSpeakTime = System.currentTimeMillis();
                SmallGameDTO smallGame = CommonGameUtil.commonGameSpeak(message);
                if (null != smallGame) {
                    int lv = CommonGameUtil.getGameRewards(smallGame.getType());
                    commonWin(tgService.getGroup(), message, lv);
                    tgService.delMsg(tgService.getGroup(), smallGame.getMsgId());
                } else {
                    TgUtil.gameSpeak(message);
                }
                Command.SPEAK_TIME_CNT.getAndDecrement();
            }
        }
    }

    /**
     * 处理修改消息
     *
     * @param message 消息
     */
    private void handleEditMsg(Message message) {
        if (!TgUtil.isGroupMsg(message)) {
            return;
        }
        Game game = GameListUtil.getGame(message.getChatId().toString());
        if (game == null || !讨论时间.equals(game.getStatus())) {
            return;
        }
        GameUser member = game.getMember(message.getFrom().getId());
        if (null == member) {
            return;
        }
        tgService.sendMsg(message.getMessageId(), message.getChatId() + "",
                StrUtil.format(WARNING_EDIT, TgUtil.tgNameOnUrl(member)));
    }

    /**
     * 处理回调查询
     *
     * @param update 更新
     */
    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        boolean isGroupMessage = callbackQuery.getMessage().isGroupMessage()
                || callbackQuery.getMessage().isSuperGroupMessage();
        gameEvent.onClick(update, isGroupMessage);
    }

    /**
     * 处理加入请求
     * <p>
     * 这里主要处理邀请进入用户
     *
     * @param joinRequest 加入请求
     */
    private void handleChatJoinRequest(ChatJoinRequest joinRequest) {
        if (joinRequest.getInviteLink() == null) {
            return;
        }
        Long tgId = joinRequest.getUser().getId();
        String inviteLink = joinRequest.getInviteLink().getInviteLink();

        // 更新db
        XInvite xInvite = xInviteDao.findByUrl(inviteLink);
        if (null == xInvite) {
            xInvite = new XInvite();
            xInvite.setUrl(inviteLink);
        }
        // 自动拒绝
        if (null != xInvite.getInviteeId()) {
            tgService.declineJoin(tgId);
            log.error("传承邀请{} 已被 {} 使用,拒绝 {} 加入", inviteLink, xInvite.getInviteeId(), tgId);
            // 过期邀请链接
            tgService.revokeInvite(inviteLink);
            return;
        }
        // 自动批准
        tgService.approveJoin(tgId);
        log.warn("传承邀请{} 已被 {} 使用,已自动批准加入", inviteLink, tgId);
        xInvite.setInviteeId(tgId);
        xInvite.setJoinTime(DateUtil.date());
        xInviteDao.updateInvitee(xInvite);
        // 过期邀请链接
        tgService.revokeInvite(inviteLink);
    }

    /**
     * 用于通用游戏 获取奖励
     *
     * @param message 消息
     * @param lv      胜利奖励
     */
    private void commonWin(String groupId, Message message, Integer lv) {
        if (lv == null || lv < 1) {
            return;
        }
        tgService.sendMsg(message.getMessageId(), groupId,
                StrUtil.format(COMMON_WIN, TgUtil.tgNameOnUrl(message.getFrom()), lv));
        embyDao.upIv(message.getFrom().getId(), lv);
    }
}