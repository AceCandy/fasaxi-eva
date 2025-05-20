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
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
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

import static cn.acecandy.fasaxi.eva.bot.game.Command.看图猜成语;
import static cn.acecandy.fasaxi.eva.bot.game.Command.看图猜番号;
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

    public final TgService tgService;
    public final Command command;
    public final GameEvent gameEvent;
    public final EmbyBossConfig embyBossConfig;
    public final EmbyDao embyDao;
    public final XInviteDao xInviteDao;

    public EmbyTelegramBot(TgService tgService,
                           @Lazy EmbyBossConfig embyBossConfig, @Lazy Command command,
                           @Lazy GameEvent gameEvent, @Lazy EmbyDao embyDao, @Lazy XInviteDao xInviteDao) {
        this.embyBossConfig = embyBossConfig;
        this.command = command;
        this.gameEvent = gameEvent;
        this.embyDao = embyDao;
        this.xInviteDao = xInviteDao;
        this.tgService = tgService;
    }

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
        // 普通消息
        Message msg = update.getMessage();
        // 修改消息
        Message editMsg = update.getEditedMessage();
        // 加入请求
        ChatJoinRequest joinRequest = update.getChatJoinRequest();
        // 按钮回调
        CallbackQuery callback = update.getCallbackQuery();
        // Console.log("update:{}", update);
        if (msg != null) {
            if (System.currentTimeMillis() / 1000 - msg.getDate() > 60) {
                log.warn("过期指令:【{}】{}", msg.getFrom().getFirstName(), msg.getText());
                return;
            }
            handleIncomingMessage(msg);
        } else if (editMsg != null) {
            handleEditMessage(editMsg);
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
    private void handleIncomingMessage(Message message) {
        boolean isCommand = message.isCommand();
        if (!message.hasText()) {
            return;
        }
        boolean isGroupMessage = message.isGroupMessage() || message.isSuperGroupMessage();
        String msgData = message.getText();
        boolean atBotUsername = msgData.endsWith(StrUtil.AT + tgService.getBotUsername());
        String msg = TgUtil.extractCommand(msgData, tgService.getBotUsername());

        if (isCommand) {
            command.process(msg, message, isGroupMessage);
        } else {
            if (isGroupMessage) {
                CommonGameUtil.endSpeakTime = System.currentTimeMillis();
                // 看图猜成语
                SmallGameDTO smallGame = CommonGameUtil.commonGameSpeak(message);
                if (null != smallGame) {
                    int lv = 0;
                    switch (smallGame.getType()) {
                        case 看图猜成语:
                            lv = RandomUtil.randomInt(4, 8);
                            break;
                        case 看图猜番号:
                            lv = RandomUtil.randomInt(8, 12);
                            break;
                        default:
                    }
                    commonWin(tgService.getGroup(), message, lv);
                    tgService.delMsg(tgService.getGroup(), smallGame.getMsgId());
                } else {
                    TgUtil.gameSpeak(message);
                }
                if (!atBotUsername) {
                    Command.SPEAK_TIME_CNT.getAndDecrement();
                }
            }
        }
    }

    /**
     * 处理修改消息
     *
     * @param message 消息
     */
    private void handleEditMessage(Message message) {
        boolean isGroupMessage = message.isGroupMessage() || message.isSuperGroupMessage();
        if (!message.hasText()) {
            return;
        }
        String msgData = message.getText();
        if (!isGroupMessage) {
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