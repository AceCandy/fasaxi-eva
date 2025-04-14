package cn.acecandy.fasaxi.eva.bot;

import cn.acecandy.fasaxi.eva.bot.game.Command;
import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.bot.game.GameEvent;
import cn.acecandy.fasaxi.eva.bot.game.GameUser;
import cn.acecandy.fasaxi.eva.common.dto.SmallGameDTO;
import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.acecandy.fasaxi.eva.config.EmbyBossConfig;
import cn.acecandy.fasaxi.eva.dao.entity.XInvite;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.XInviteDao;
import cn.acecandy.fasaxi.eva.utils.CommonGameUtil;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.acecandy.fasaxi.eva.utils.MsgDelUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ApproveChatJoinRequest;
import org.telegram.telegrambots.meta.api.methods.groupadministration.CreateChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.groupadministration.DeclineChatJoinRequest;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RevokeChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatPermissions;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatInviteLink;
import org.telegram.telegrambots.meta.api.objects.ChatJoinRequest;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static cn.acecandy.fasaxi.eva.bot.game.Command.看图猜成语;
import static cn.acecandy.fasaxi.eva.bot.game.Command.看图猜番号;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.COMMON_WIN;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.WARNING_EDIT;
import static cn.acecandy.fasaxi.eva.common.enums.GameStatus.讨论时间;
import static cn.hutool.core.text.StrPool.COMMA;

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

    public final TelegramClient tgClient;
    public final Command command;
    public final GameEvent gameEvent;
    public final EmbyBossConfig embyBossConfig;
    public final EmbyDao embyDao;
    public final XInviteDao xInviteDao;

    public EmbyTelegramBot(@Lazy EmbyBossConfig embyBossConfig, @Lazy Command command,
                           @Lazy GameEvent gameEvent, @Lazy EmbyDao embyDao, @Lazy XInviteDao xInviteDao) {
        this.embyBossConfig = embyBossConfig;
        this.command = command;
        this.gameEvent = gameEvent;
        this.embyDao = embyDao;
        this.xInviteDao = xInviteDao;
        this.tgClient = new OkHttpTelegramClient(getBotToken());
    }

    @Override
    public String getBotToken() {
        return embyBossConfig.getToken();
    }

    public String getBotUsername() {
        return embyBossConfig.getName();
    }

    public Long getOwners() {
        return Long.parseLong(embyBossConfig.getOwners());
    }

    public List<Long> getAdmins() {
        return StrUtil.splitTrim(embyBossConfig.getAdmins(), COMMA)
                .stream().map(Long::parseLong).toList();
    }

    public Long getGroup() {
        return embyBossConfig.getGroup();
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
            // handleEditMessage(editMsg);
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
        boolean atBotUsername = msgData.endsWith(StrUtil.AT + getBotUsername());
        String msg = TgUtil.extractCommand(msgData, getBotUsername());

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
                    commonWin(getGroup(), message, lv);
                    deleteMessage(getGroup(), smallGame.getMsgId());
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
        Game game = GameListUtil.getGame(message.getChatId());
        if (game == null || !讨论时间.equals(game.getStatus())) {
            return;
        }
        GameUser member = game.getMember(message.getFrom().getId());
        if (null == member) {
            return;
        }
        sendMessage(message.getMessageId(), message.getChatId(),
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
            declineJoin(tgId);
            log.error("传承邀请{} 已被 {} 使用,拒绝 {} 加入", inviteLink, xInvite.getInviteeId(), tgId);
            // 过期邀请链接
            revokeInvite(inviteLink);
            return;
        }
        // 自动批准
        approveJoin(tgId);
        log.warn("传承邀请{} 已被 {} 使用,已自动批准加入", inviteLink, tgId);
        xInvite.setInviteeId(tgId);
        xInvite.setJoinTime(DateUtil.date());
        xInviteDao.updateInvitee(xInvite);
        // 过期邀请链接
        revokeInvite(inviteLink);
    }

    @FunctionalInterface
    private interface TelegramOperation<T> {
        T execute() throws TelegramApiException;
    }

    @SneakyThrows
    @Retryable(retryFor = {TelegramApiException.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    private <T> T executeTg(TelegramOperation<T> operation) {
        return operation.execute();
    }

    public void editMessage(MaybeInaccessibleMessage message,
                            String newCaption, InlineKeyboardMarkup replyMarkup) {
        EditMessageCaption editMessageText = new EditMessageCaption();
        editMessageText.setCaption(newCaption);
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setReplyMarkup(replyMarkup);
        editMessageText.setParseMode(ParseMode.HTML);
        executeTg(() -> tgClient.executeAsync(editMessageText));
    }

    public void editMessage(MaybeInaccessibleMessage message, String newCaption) {
        EditMessageCaption editMessageText = new EditMessageCaption();
        editMessageText.setCaption(newCaption);
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setParseMode(ParseMode.HTML);
        executeTg(() -> tgClient.executeAsync(editMessageText));
    }

    public Message sendMessage(SendMessage message) {
        message.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(message));
    }

    public Message sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(message));
    }

    public Message sendMessage(Integer replyId, Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setReplyToMessageId(replyId);
        message.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(message));
    }

    public void sendMessage(Integer replyId, Long chatId, String text, long autoDeleteTime) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setReplyToMessageId(replyId);
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        MsgDelUtil.addAutoDeleteMessage(execute, autoDeleteTime);
    }

    public void sendMessage(Long chatId, String text, long autoDeleteTime) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        MsgDelUtil.addAutoDeleteMessage(execute, autoDeleteTime);
    }

    public Message sendAnimation(SendAnimation photo) {
        photo.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(photo));
    }

    public Message sendPhoto(SendPhoto photo) {
        photo.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(photo));
    }

    public Message sendPhoto(SendPhoto photo, long autoDeleteTime) {
        photo.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(photo));
        MsgDelUtil.addAutoDeleteMessage(execute, autoDeleteTime);
        return execute;
    }

    public Message sendPhoto(SendPhoto photo, long autoDeleteTime, String commonGameType) {
        photo.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(photo));
        MsgDelUtil.addAutoDeleteMessage(execute, autoDeleteTime, commonGameType);
        return execute;
    }

    public Message sendPhoto(SendPhoto photo, long autoDeleteTime, GameStatus status, Game game) {
        photo.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(photo));
        MsgDelUtil.addAutoDeleteMessage(execute, autoDeleteTime, status, game);
        return execute;
    }

    public void sendMessage(SendMessage message, long autoDeleteTime) {
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        MsgDelUtil.addAutoDeleteMessage(execute, autoDeleteTime);
    }

    public Message sendMessage(SendMessage message, long autoDeleteTime,
                               GameStatus status, Game game) {
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        MsgDelUtil.addAutoDeleteMessage(execute, autoDeleteTime, status, game);
        return execute;
    }

    public Message sendMessage(SendMessage message, GameStatus status, Game game) {
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        MsgDelUtil.addAutoDeleteMessage(execute, status, game);
        return execute;
    }

    /**
     * 生成邀请
     *
     * @param inviteLink 链接
     * @return {@link String }
     */
    public String generateInvite(CreateChatInviteLink inviteLink) {
        return executeTg(() -> tgClient.execute(inviteLink)).getInviteLink();
    }

    /**
     * 生成邀请
     * <p>
     * 默认1天有效期
     *
     * @param userId      用户
     * @param memberLimit 会员限额
     * @return {@link String }
     */
    public String generateInvite(Long userId, Integer memberLimit) {
        Integer expireDate = (int) DateUtil.currentSeconds() + 86400;
        String inviteName = userId + "_" + memberLimit + "_" + DateUtil.now();
        CreateChatInviteLink inviteLink = new CreateChatInviteLink(
                getGroup().toString(), expireDate, null, inviteName, true);
        ChatInviteLink result = executeTg(() -> tgClient.execute(inviteLink));
        return result.getInviteLink();
    }

    /**
     * 撤销邀请
     *
     * @param link 链接
     */
    public void revokeInvite(String link) {
        link = StrUtil.removePrefixIgnoreCase(link, "https://t.me/+");
        RevokeChatInviteLink inviteLink = new RevokeChatInviteLink(getGroup().toString(), link);
        executeTg(() -> tgClient.execute(inviteLink));
    }

    /**
     * 自动批准加入
     *
     * @param userId 用户
     * @return {@link String }
     */
    public Boolean approveJoin(Long userId) {
        ApproveChatJoinRequest approve = new ApproveChatJoinRequest(getGroup().toString(), userId);
        return executeTg(() -> tgClient.execute(approve));
    }

    /**
     * 拒绝加入
     *
     * @param userId 用户id
     * @return {@link Boolean }
     */
    public Boolean declineJoin(Long userId) {
        DeclineChatJoinRequest decline = new DeclineChatJoinRequest(getGroup().toString(), userId);
        return executeTg(() -> tgClient.execute(decline));
    }

    public void muteGroup(Long chatId) {
        ChatPermissions permissions = ChatPermissions.builder()
                .canSendMessages(false).build();
        SetChatPermissions setChatPermissions = new SetChatPermissions(chatId.toString(), permissions);
        executeTg(() -> tgClient.executeAsync(setChatPermissions));
    }

    public void unmuteGroup(Long chatId) {
        ChatPermissions permissions = ChatPermissions.builder()
                .canSendMessages(true).canSendPhotos(true).canSendOtherMessages(true).build();
        SetChatPermissions setChatPermissions = new SetChatPermissions(chatId.toString(), permissions);
        executeTg(() -> tgClient.executeAsync(setChatPermissions));
    }

    public void pinMsg(Long chatId, Integer msgId) {
        PinChatMessage msg = new PinChatMessage(chatId.toString(), msgId);
        msg.setDisableNotification(false);
        executeTg(() -> tgClient.executeAsync(msg));
    }

    public void unPinMsg(Long chatId, Integer msgId) {
        UnpinChatMessage msg = new UnpinChatMessage(chatId.toString());
        msg.setMessageId(msgId);
        executeTg(() -> tgClient.executeAsync(msg));
    }

    public void deleteMessage(Message message) {
        DeleteMessage msg = new DeleteMessage(message.getChatId() + "", message.getMessageId());
        executeTg(() -> tgClient.executeAsync(msg));
    }

    public void deleteMessage(Long chatId, Integer msgId) {
        DeleteMessage msg = new DeleteMessage(chatId + "", msgId);
        executeTg(() -> tgClient.executeAsync(msg));
    }

    public void sendCallback(AnswerCallbackQuery callback) {
        executeTg(() -> tgClient.executeAsync(callback));
    }

    public void setCommand() {
        SetMyCommands setMyCommands = new SetMyCommands(List.of(
                new BotCommand("/wd", "创建游戏(10Dmail)"),
                new BotCommand("/wd_info", "查看自身游戏积分记录(2Dmail)"),
                new BotCommand("/wd_rank", "翻阅游戏积分排行榜(15Dmail)"),
                new BotCommand("/wd_top", "翻阅首飞霸王榜(10Dmail)"),
                new BotCommand("/wd_exit", "关闭游戏(3Dmail)"),
                new BotCommand("/wd_help", "获取帮助")
        ));
        setMyCommands.setScope(new BotCommandScopeAllGroupChats());
        executeTg(() -> tgClient.executeAsync(setMyCommands));
        log.info("初始化bot指令成功！");
    }

    /**
     * 用于通用游戏 获取奖励
     *
     * @param message 消息
     * @param lv      胜利奖励
     */
    private void commonWin(Long groupId, Message message, Integer lv) {
        if (lv == null || lv < 1) {
            return;
        }
        sendMessage(message.getMessageId(), groupId,
                StrUtil.format(COMMON_WIN, TgUtil.tgNameOnUrl(message.getFrom()), lv));
        embyDao.upIv(message.getFrom().getId(), lv);
    }
}