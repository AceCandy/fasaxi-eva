package cn.acecandy.fasaxi.eva.bot.impl;

import cn.hutool.core.util.StrUtil;
import cn.acecandy.fasaxi.eva.bin.GameStatus;
import cn.acecandy.fasaxi.eva.bin.TgUtil;
import cn.acecandy.fasaxi.eva.config.EmbyBossConfig;
import cn.acecandy.fasaxi.eva.game.Game;
import cn.acecandy.fasaxi.eva.runtime.Task;
import cn.acecandy.fasaxi.eva.service.ButtonEvent;
import cn.acecandy.fasaxi.eva.service.Command;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
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
    public final ButtonEvent buttonEvent;
    public final EmbyBossConfig embyBossConfig;

    public EmbyTelegramBot(@Lazy EmbyBossConfig embyBossConfig,
                           @Lazy Command command, @Lazy ButtonEvent buttonEvent) {
        this.embyBossConfig = embyBossConfig;
        this.command = command;
        this.buttonEvent = buttonEvent;
        this.tgClient = new OkHttpTelegramClient(getBotToken());
    }

    @Override
    public String getBotToken() {
        // return PropsUtil.get("telegram").getProperty("token");
        return embyBossConfig.getToken();
    }

    public String getBotUsername() {
        // return PropsUtil.get("telegram").getProperty("username");
        return embyBossConfig.getName();
    }

    public Long getOwners() {
        // return Long.parseLong(PropsUtil.get("telegram").getProperty("owners"));
        return Long.parseLong(embyBossConfig.getOwners());
    }

    public List<Long> getAdmins() {
        return StrUtil.splitTrim(embyBossConfig.getAdmins(), COMMA)
                .stream().map(Long::parseLong).toList();
    }

    public String getGroup() {
        // return PropsUtil.get("telegram").getProperty("group");
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
        // handleChatMembers(message);
        if (!atBotUsername && !isCommand && isGroupMessage) {
            // 全局游戏开局cnt-1
            Command.SPEAK_TIME_CNT.getAndDecrement();
            TgUtil.gameSpeak(message);
        }
    }

    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        boolean isGroupMessage = callbackQuery.getMessage().isGroupMessage()
                || callbackQuery.getMessage().isSuperGroupMessage();
        buttonEvent.onClick(update, isGroupMessage);
    }


    @SneakyThrows
    public void editMessage(MaybeInaccessibleMessage message, String newCaption, InlineKeyboardMarkup replyMarkup) {
        EditMessageCaption editMessageText = new EditMessageCaption();
        // EditMessageText editMessageText = new EditMessageText(newMessageText);
        editMessageText.setCaption(newCaption);
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setReplyMarkup(replyMarkup);
        editMessageText.setParseMode(ParseMode.HTML);
        tgClient.executeAsync(editMessageText);
    }

    @SneakyThrows
    public Message sendMessage(SendMessage message) {
        message.setParseMode(ParseMode.HTML);
        return tgClient.execute(message);
    }

    @SneakyThrows
    public Message sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setParseMode(ParseMode.HTML);
        return tgClient.execute(message);
    }

    @SneakyThrows
    public Message sendPhoto(SendPhoto photo, long autoDeleteTime) {
        Message execute = tgClient.execute(photo);
        Task.addAutoDeleteMessage(execute, autoDeleteTime);
        return execute;
    }


    @SneakyThrows
    public Message sendPhoto(SendPhoto photo, long autoDeleteTime, GameStatus status, Game game) {
        Message execute = tgClient.execute(photo);
        Task.addAutoDeleteMessage(execute, autoDeleteTime, status, game);
        return execute;
    }

    @SneakyThrows
    public void sendMessage(SendMessage message, long autoDeleteTime) {
        message.setParseMode(ParseMode.HTML);
        Message execute = tgClient.execute(message);
        Task.addAutoDeleteMessage(execute, autoDeleteTime);
    }

    @SneakyThrows
    public Message sendMessage(SendMessage message, long autoDeleteTime, GameStatus status, Game game) {
        message.setParseMode(ParseMode.HTML);
        Message execute = tgClient.execute(message);
        Task.addAutoDeleteMessage(execute, autoDeleteTime, status, game);
        return execute;
    }

    @SneakyThrows
    public void pinMsg(Long chatId, Integer msgId) {
        PinChatMessage msg = new PinChatMessage(chatId.toString(), msgId);
        msg.setDisableNotification(true);
        tgClient.execute(msg);
    }

    @SneakyThrows
    public void deleteMessage(Message message) {
        tgClient.executeAsync(new DeleteMessage(message.getChatId() + "", message.getMessageId()));
    }

    @SneakyThrows
    public void sendCallback(AnswerCallbackQuery callback) {
        tgClient.executeAsync(callback);
    }

}