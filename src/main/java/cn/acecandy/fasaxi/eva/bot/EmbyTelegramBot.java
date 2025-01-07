package cn.acecandy.fasaxi.eva.bot;

import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.acecandy.fasaxi.eva.config.EmbyBossConfig;
import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.bot.game.GameEvent;
import cn.acecandy.fasaxi.eva.bot.game.Command;
import cn.acecandy.fasaxi.eva.task.ScheduledTask;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
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
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatPermissions;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

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

    public EmbyTelegramBot(@Lazy EmbyBossConfig embyBossConfig,
                           @Lazy Command command, @Lazy GameEvent gameEvent) {
        this.embyBossConfig = embyBossConfig;
        this.command = command;
        this.gameEvent = gameEvent;
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

    public String getGroup() {
        return embyBossConfig.getGroup();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.getMessage() != null) {
            Integer date = update.getMessage().getDate();
            if (System.currentTimeMillis() / 1000 - date > 60) {
                log.warn("过期指令: {} {}", update.getMessage().getText(), update.getMessage());
                return;
            }
            handleIncomingMessage(update.getMessage());
        } else if (update.getCallbackQuery() != null) {
            handleCallbackQuery(update);
        }
    }

    private void handleIncomingMessage(Message message) {
        boolean isGroupMessage = message.isGroupMessage() || message.isSuperGroupMessage();
        boolean isCommand = false;
        boolean atBotUsername = false;
        if (!message.hasText()) {
            return;
        }

        String msgData = message.getText();
        String msg = TgUtil.extractCommand(msgData, getBotUsername());
        if (msgData.endsWith(StrUtil.AT + getBotUsername())) {
            atBotUsername = true;
        }

        if (message.isCommand()) {
            isCommand = true;
            command.process(msg, message, isGroupMessage);
        }

        if (!atBotUsername && !isCommand && isGroupMessage) {
            Command.SPEAK_TIME_CNT.getAndDecrement();
            TgUtil.gameSpeak(message);
        }
    }

    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        boolean isGroupMessage = callbackQuery.getMessage().isGroupMessage()
                || callbackQuery.getMessage().isSuperGroupMessage();
        gameEvent.onClick(update, isGroupMessage);
    }

    @FunctionalInterface
    private interface TelegramOperation<T> {
        T execute() throws TelegramApiException;
    }

    @SneakyThrows
    @Retryable(retryFor = {TelegramApiException.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
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

    public Message sendMessage(SendMessage message) {
        message.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(message));
    }

    public Message sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setParseMode(ParseMode.HTML);
        return executeTg(() -> tgClient.execute(message));
    }

    public Message sendPhoto(SendPhoto photo, long autoDeleteTime) {
        Message execute = executeTg(() -> tgClient.execute(photo));
        ScheduledTask.addAutoDeleteMessage(execute, autoDeleteTime);
        return execute;
    }

    public Message sendPhoto(SendPhoto photo, long autoDeleteTime, GameStatus status, Game game) {
        Message execute = executeTg(() -> tgClient.execute(photo));
        ScheduledTask.addAutoDeleteMessage(execute, autoDeleteTime, status, game);
        return execute;
    }

    public void sendMessage(SendMessage message, long autoDeleteTime) {
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        ScheduledTask.addAutoDeleteMessage(execute, autoDeleteTime);
    }

    public Message sendMessage(SendMessage message, long autoDeleteTime,
                               GameStatus status, Game game) {
        message.setParseMode(ParseMode.HTML);
        Message execute = executeTg(() -> tgClient.execute(message));
        ScheduledTask.addAutoDeleteMessage(execute, autoDeleteTime, status, game);
        return execute;
    }

    public void muteGroup(Long chatId) {
        ChatPermissions permissions = ChatPermissions.builder()
                .canSendMessages(false).build();
        SetChatPermissions setChatPermissions = new SetChatPermissions(chatId.toString(), permissions);
        executeTg(() -> tgClient.executeAsync(setChatPermissions));
    }

    public void unmuteGroup(Long chatId) {
        ChatPermissions permissions = ChatPermissions.builder()
                .canSendMessages(true).build();
        SetChatPermissions setChatPermissions = new SetChatPermissions(chatId.toString(), permissions);
        executeTg(() -> tgClient.executeAsync(setChatPermissions));
    }

    public void pinMsg(Long chatId, Integer msgId) {
        PinChatMessage msg = new PinChatMessage(chatId.toString(), msgId);
        msg.setDisableNotification(true);
        executeTg(() -> tgClient.executeAsync(msg));
    }

    public void deleteMessage(Message message) {
        DeleteMessage msg = new DeleteMessage(message.getChatId() + "", message.getMessageId());
        executeTg(() -> tgClient.executeAsync(msg));
    }

    public void sendCallback(AnswerCallbackQuery callback) {
        executeTg(() -> tgClient.executeAsync(callback));
    }

}