package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.acecandy.fasaxi.eva.config.EmbyBossConfig;
import cn.acecandy.fasaxi.eva.utils.MsgDelUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ApproveChatJoinRequest;
import org.telegram.telegrambots.meta.api.methods.groupadministration.CreateChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.groupadministration.DeclineChatJoinRequest;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RevokeChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatPermissions;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.ChatInviteLink;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static cn.hutool.core.text.StrPool.COMMA;

/**
 * tg操作 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class TgService {

    private final TelegramClient tgClient;
    private final EmbyBossConfig config;

    public TgService(EmbyBossConfig config) {
        this.config = config;
        // this.tgClient = new OkHttpTelegramClient(new OkHttpClient.Builder()
        //         .proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("192.168.1.205", 7890)))
        //         .build(), config.getToken());
        this.tgClient = new OkHttpTelegramClient(config.getToken());
    }

    public String getBotUsername() {
        return config.getName();
    }

    public Long getOwners() {
        return Long.parseLong(config.getOwners());
    }

    public List<Long> getAdmins() {
        return StrUtil.splitTrim(config.getAdmins(), COMMA)
                .stream().map(Long::parseLong).toList();
    }

    public String getGroup() {
        return config.getGroup();
    }

    @FunctionalInterface
    private interface TelegramOperation<T> {
        T execute() throws TelegramApiException;
    }

    @SneakyThrows
    // @Retryable(retryFor = {TelegramApiException.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    private <T> T executeTg(TelegramOperation<T> operation) {
        return operation.execute();
    }

    public void editMsg(MaybeInaccessibleMessage message,
                        String newCaption, InlineKeyboardMarkup replyMarkup) {
        EditMessageCaption editMessageText = new EditMessageCaption();
        editMessageText.setCaption(newCaption);
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setReplyMarkup(replyMarkup);
        editMessageText.setParseMode(ParseMode.HTML);
        executeTg(() -> tgClient.executeAsync(editMessageText));
    }

    public void editMsg(MaybeInaccessibleMessage message, String newCaption) {
        EditMessageCaption editMessageText = new EditMessageCaption();
        editMessageText.setCaption(newCaption);
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setParseMode(ParseMode.HTML);
        executeTg(() -> tgClient.executeAsync(editMessageText));
    }

    public Message sendMsg(SendMessage message) {
        message.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(message));
    }

    public Message sendMsg(String chatId, String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        SendMessage message = new SendMessage(chatId, text);
        message.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(message));
    }

    public Message sendMsg(Integer replyId, String chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        message.setReplyToMessageId(replyId);
        message.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(message));
    }

    public void sendMsg(Integer replyId, Long chatId, String text, long autoDeleteTime) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setReplyToMessageId(replyId);
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        MsgDelUtil.addAutoDelMsg(execute, autoDeleteTime);
    }

    public void sendMsg(String chatId, String text, long autoDeleteTime) {
        SendMessage message = new SendMessage(chatId, text);
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        MsgDelUtil.addAutoDelMsg(execute, autoDeleteTime);
    }

    public Message sendAnimation(SendAnimation photo) {
        photo.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(photo));
    }

    public Message sendAnimation(SendAnimation photo, long autoDeleteTime) {
        photo.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(photo));
        MsgDelUtil.addAutoDelMsg(execute, autoDeleteTime);
        return execute;
    }

    public Message sendPhoto(SendPhoto photo) {
        photo.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(photo));
    }

    public Message sendPhoto(SendPhoto photo, long autoDeleteTime) {
        photo.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(photo));
        MsgDelUtil.addAutoDelMsg(execute, autoDeleteTime);
        return execute;
    }

    public Message sendPhoto(SendPhoto photo, long autoDeleteTime, String commonGameType) {
        photo.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(photo));
        MsgDelUtil.addAutoDelMsg(execute, autoDeleteTime, commonGameType);
        return execute;
    }

    public Message sendPhoto(SendPhoto photo, long autoDeleteTime, GameStatus status, Game game) {
        photo.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(photo));
        MsgDelUtil.addAutoDelMsg(execute, autoDeleteTime, status, game);
        return execute;
    }

    public void sendMsg(SendMessage message, long autoDeleteTime) {
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        MsgDelUtil.addAutoDelMsg(execute, autoDeleteTime);
    }

    public Message sendMsg(SendMessage message, long autoDeleteTime,
                           GameStatus status, Game game) {
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        MsgDelUtil.addAutoDelMsg(execute, autoDeleteTime, status, game);
        return execute;
    }

    public Message sendMsg(SendMessage message, GameStatus status, Game game) {
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        MsgDelUtil.addAutoDelMsg(execute, status, game);
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
                getGroup(), expireDate, null, inviteName, true);
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
        RevokeChatInviteLink inviteLink = new RevokeChatInviteLink(getGroup(), link);
        executeTg(() -> tgClient.execute(inviteLink));
    }

    /**
     * 自动批准加入
     *
     * @param userId 用户
     * @return {@link String }
     */
    public Boolean approveJoin(Long userId) {
        ApproveChatJoinRequest approve = new ApproveChatJoinRequest(getGroup(), userId);
        return executeTg(() -> tgClient.execute(approve));
    }

    /**
     * 拒绝加入
     *
     * @param userId 用户id
     * @return {@link Boolean }
     */
    public Boolean declineJoin(Long userId) {
        DeclineChatJoinRequest decline = new DeclineChatJoinRequest(getGroup(), userId);
        return executeTg(() -> tgClient.execute(decline));
    }

    /**
     * 禁言群组
     *
     * @param chatId 聊天id
     */
    public void muteGroup(String chatId) {
        ChatPermissions permissions = ChatPermissions.builder()
                .canSendMessages(false).build();
        SetChatPermissions setChatPermissions = new SetChatPermissions(chatId, permissions);
        executeTg(() -> tgClient.executeAsync(setChatPermissions));
    }

    public void unmuteGroup(String chatId) {
        ChatPermissions permissions = ChatPermissions.builder()
                .canSendMessages(true).canSendPhotos(true).canSendOtherMessages(true).build();
        SetChatPermissions setChatPermissions = new SetChatPermissions(chatId, permissions);
        executeTg(() -> tgClient.executeAsync(setChatPermissions));
    }

    public void pinMsg(String chatId, Integer msgId) {
        PinChatMessage msg = new PinChatMessage(chatId, msgId);
        msg.setDisableNotification(false);
        executeTg(() -> tgClient.executeAsync(msg));
    }

    public void unPinMsg(String chatId, Integer msgId) {
        UnpinChatMessage msg = new UnpinChatMessage(chatId);
        msg.setMessageId(msgId);
        executeTg(() -> tgClient.executeAsync(msg));
    }

    public void delMsg(Message message) {
        DeleteMessage msg = new DeleteMessage(message.getChatId() + "", message.getMessageId());
        executeTg(() -> tgClient.executeAsync(msg));
    }

    public void delMsg(String chatId, Integer msgId) {
        DeleteMessage msg = new DeleteMessage(chatId, msgId);
        executeTg(() -> tgClient.executeAsync(msg));
    }

    /**
     * 获取用户在该群组状态
     *
     * @param chatId 聊天id
     * @param userId 用户id
     * @return {@link ChatMember }
     */
    public ChatMember getChatMember(String chatId, Long userId) {
        GetChatMember getChatMember = new GetChatMember(chatId, userId);
        return executeTg(() -> tgClient.execute(getChatMember));
    }

    public void sendCallback(AnswerCallbackQuery callback) {
        executeTg(() -> tgClient.executeAsync(callback));
    }

    public void setCommand() {
        SetMyCommands setMyCommands = new SetMyCommands(List.of(
                new BotCommand("/red", "创建红包"),
                new BotCommand("/wd", "创建游戏(10Dmail)"),
                new BotCommand("/wd_checkin", "签到并查看个人信息(5Dmail[3分钟内无消耗])"),
                new BotCommand("/wd_rank", "开启积分榜(10Dmail)"),
                new BotCommand("/wd_real_rank", "开启实时战力榜(10Dmail)"),
                new BotCommand("/wd_top", "开启登顶霸王榜(3Dmail)"),
                new BotCommand("/wd_exit", "关闭游戏(10Dmail)"),
                new BotCommand("/wd_help", "获取帮助")
        ));
        setMyCommands.setScope(new BotCommandScopeAllGroupChats());
        executeTg(() -> tgClient.executeAsync(setMyCommands));
        log.info("初始化群bot指令成功！");
        SetMyCommands setPrivateCommands = new SetMyCommands(List.of(
                new BotCommand("/wd_help", "获取帮助")
        ));
        setPrivateCommands.setScope(new BotCommandScopeAllPrivateChats());
        executeTg(() -> tgClient.executeAsync(setPrivateCommands));
        log.info("初始化私人bot指令成功！");
    }
}