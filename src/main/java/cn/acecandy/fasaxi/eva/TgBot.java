package cn.acecandy.fasaxi.eva;

import cn.acecandy.fasaxi.eva.bot.game.Command;
import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.bot.game.GameEvent;
import cn.acecandy.fasaxi.eva.bot.game.GameUser;
import cn.acecandy.fasaxi.eva.common.ex.BaseException;
import cn.acecandy.fasaxi.eva.config.CommonGameConfig;
import cn.acecandy.fasaxi.eva.config.EmbyBossConfig;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserDao;
import cn.acecandy.fasaxi.eva.task.impl.CcService;
import cn.acecandy.fasaxi.eva.task.impl.GameService;
import cn.acecandy.fasaxi.eva.task.impl.RedService;
import cn.acecandy.fasaxi.eva.task.impl.TgService;
import cn.acecandy.fasaxi.eva.task.impl.WdService;
import cn.acecandy.fasaxi.eva.task.impl.XmService;
import cn.acecandy.fasaxi.eva.utils.CommandUtil;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.acecandy.fasaxi.eva.utils.GlobalUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatJoinRequest;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.NO_AUTH_GROUP;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.WARNING_EDIT;
import static cn.acecandy.fasaxi.eva.common.enums.GameStatus.讨论时间;

/**
 * emby电报机器人
 *
 * @author AceCandy
 * @since 2024/10/21
 */
// @Lazy
@Slf4j
@Component
public class TgBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    @Resource
    public TgService tgService;
    @Resource
    public Command command;
    @Resource
    public WdService wdService;
    @Resource
    public GameService gameService;
    @Resource
    public EmbyBossConfig embyBossConfig;
    @Resource
    public GameEvent gameEvent;
    @Resource
    public CcService ccService;
    @Resource
    public XmService xmService;
    @Resource
    public CommonGameConfig commonGameConfig;
    @Resource
    private EmbyDao embyDao;
    @Resource
    private WodiUserDao wodiUserDao;
    @Resource
    private RedService redService;

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

        if (TgUtil.isNewMember(msg) && commonGameConfig.getGroupDoor().getEnable()) {
            handleNewMember(msg);
        } else if (TgUtil.isLeftMember(msg) && commonGameConfig.getGroupDoor().getEnable()) {
            handleLeftMember(msg);
        } else if (TgUtil.isMessageValid(msg)) {
            handleMsg(msg);
        } else if (editMsg != null) {
            handleEditMsg(editMsg);
        } else if (callback != null) {
            handleCallbackQuery(callback);
        } else if (joinRequest != null) {
            handleChatJoinRequest(joinRequest);
        } else {
            handleOtherMsg(msg);
        }
    }

    /**
     * 处理新加入成员
     *
     * @param message 消息
     */
    private void handleNewMember(Message message) {
        List<User> newUsers = message.getNewChatMembers();
        newUsers.forEach(user -> {
            Emby emby = embyDao.findByTgId(user.getId());
            int iv = 0;
            if (emby != null && StrUtil.equals(emby.getLv(), "e")) {
                WodiUser wodi = wodiUserDao.findByTgId(user.getId());
                iv = (int) ((emby.getIv() + 3 * wodi.getFraction()) * 0.1);

                tgService.sendMsg(message.getChatId().toString(), StrUtil.format("恭喜外门弟子{}进入内门, " +
                        "您的本金已经转化为{} Dmail, 注意去新bot中查看！", TgUtil.tgName(user), iv));
            }
            embyDao.init(user.getId(), iv);
        });
    }

    /**
     * 处理离开成员
     *
     * @param message 消息
     */
    private void handleLeftMember(Message message) {
        User user = message.getLeftChatMember();
        if (null == user) {
            return;
        }
        embyDao.destory(user.getId());
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
        wdService.needDel(message);
    }

    /**
     * 处理传入消息
     *
     * @param message 消息
     */
    private void handleOtherMsg(Message message) {
        wdService.needDel(message);
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
        tgService.delMsg(message);
        String cmd = TgUtil.extractCommand(message.getText(), tgService.getBotUsername());
        if (StrUtil.equals(cmd, "/start")) {
            if (!commonGameConfig.getInit().getEnable()) {
                return;
            }
            Emby embyUser = embyDao.findByTgId(message.getFrom().getId());
            WodiUser wodiUser = wodiUserDao.findByTgId(message.getFrom().getId());
            if (embyUser != null && wodiUser != null) {
                return;
            }
            if (embyUser == null) {
                embyDao.destory(message.getFrom().getId());
            }
            if (wodiUser == null) {
                wodiUser = new WodiUser();
                wodiUser.setFirstName(message.getFrom().getFirstName());
                wodiUser.setLastName(message.getFrom().getLastName());
                wodiUser.setUserName(message.getFrom().getUserName());
                wodiUser.setTelegramId(message.getFrom().getId());
                wodiUserDao.insertOrUpdate(wodiUser);
            }
            tgService.sendMsg(message.getChatId().toString(), "✅初始化Game账号成功！", 5 * 1000);
        } else if (CommandUtil.isRedCommand(cmd)) {
            if (!commonGameConfig.getRed().getEnable()) {
                return;
            }
            redService.process(cmd, message);
        } else if (CommandUtil.isWdCommand(cmd)) {
            if (!commonGameConfig.getWd().getEnable()) {
                tgService.sendMsg(message.getChatId().toString(), "该bot未开启该功能！", 5 * 1000);
                return;
            }
            wdService.process(cmd, message);
        } else if (CommandUtil.isCcCommand(cmd)) {
            if (!commonGameConfig.getCc().getEnable()) {
                tgService.sendMsg(message.getChatId().toString(), "该bot未开启该功能！", 5 * 1000);
                return;
            }
            ccService.process(cmd, message);
        } else if (CommandUtil.isXmCommand(cmd)) {
            if (!commonGameConfig.getXm().getEnable()) {
                tgService.sendMsg(message.getChatId().toString(), "该bot未开启该功能！", 5 * 1000);
                return;
            }
            xmService.process(cmd, message);
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
        if (!TgUtil.isGroupMsg(message) || message.isCommand()) {
            return;
        }
        try {
            gameService.speak(message);
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
        if (null == member || !member.survive) {
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