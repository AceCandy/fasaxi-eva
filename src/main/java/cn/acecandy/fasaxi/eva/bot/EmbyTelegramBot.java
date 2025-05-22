package cn.acecandy.fasaxi.eva.bot;

import cn.acecandy.fasaxi.eva.bot.game.Command;
import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.bot.game.GameEvent;
import cn.acecandy.fasaxi.eva.bot.game.GameUser;
import cn.acecandy.fasaxi.eva.common.ex.BaseException;
import cn.acecandy.fasaxi.eva.config.EmbyBossConfig;
import cn.acecandy.fasaxi.eva.task.impl.CcService;
import cn.acecandy.fasaxi.eva.task.impl.CommonGameService;
import cn.acecandy.fasaxi.eva.task.impl.TgService;
import cn.acecandy.fasaxi.eva.task.impl.WdService;
import cn.acecandy.fasaxi.eva.utils.CommandUtil;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.acecandy.fasaxi.eva.utils.GlobalUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
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

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.NO_AUTH_GROUP;
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
    public WdService wdService;
    @Resource
    public CommonGameService commonGameService;
    @Resource
    public EmbyBossConfig embyBossConfig;
    @Resource
    public GameEvent gameEvent;
    @Resource
    public CcService ccService;

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
        try {
            handleUpdate(update);
        } catch (BaseException e) {
            log.warn("业务提示: {}", ExceptionUtil.getSimpleMessage(e));
        } catch (Exception e) {
            log.error("未知异常: ", e);
        }
    }

    private void handleUpdate(Update update) {
        // Console.log(JSONUtil.toJsonStr(update));
        Message msg = update.getMessage();
        Message editMsg = update.getEditedMessage();
        ChatJoinRequest joinRequest = update.getChatJoinRequest();
        CallbackQuery callback = update.getCallbackQuery();

        if (TgUtil.isMessageValid(msg)) {
            handleMsg(msg);
        } else if (editMsg != null) {
            handleEditMsg(editMsg);
        } else if (callback != null) {
            handleCallbackQuery(callback);
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
        checkGroupAuth(message);
        processCommand(message);
        processSpeak(message);
    }

    /**
     * 检查是否拥有群组权限
     *
     * @param message 消息
     */
    private void checkGroupAuth(Message message) {
        if (!TgUtil.isGroupMsg(message)) {
            return;
        }
        String chatId = message.getChatId().toString();
        if (StrUtil.equals(chatId, tgService.getGroup())) {
            return;
        }
        tgService.sendMsg(chatId, NO_AUTH_GROUP);
        throw new BaseException(StrUtil.format("非授权群组私自拉bot入群已被发现：{}, chat: {}",
                chatId, JSONUtil.toJsonStr(message.getChat())));
    }

    /**
     * 处理命令
     *
     * @param message 消息
     */
    private void processCommand(Message message) {
        if (!message.isCommand()) {
            return;
        }
        String cmd = TgUtil.extractCommand(message.getText(), tgService.getBotUsername());
        if (CommandUtil.isWdCommand(cmd)) {
            wdService.process(cmd, message);
        } else {
            command.process(cmd, message);
        }
    }

    /**
     * 处理聊天
     *
     * @param message 消息
     */
    private void processSpeak(Message message) {
        if (!TgUtil.isGroupMsg(message)) {
            return;
        }
        try {
            commonGameService.speak(message);
            wdService.speak(message);
        } finally {
            GlobalUtil.updateSpeak();
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
     * @param callback 更新
     */
    private void handleCallbackQuery(CallbackQuery callback) {
        if (!JSONUtil.isTypeJSON(callback.getData())) {
            return;
        }
        gameEvent.onClick(callback);
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
        ccService.autoApprove(joinRequest);
    }
}