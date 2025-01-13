package cn.acecandy.fasaxi.eva.bot.game;

import cn.acecandy.fasaxi.eva.bot.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.NO_EMBY_USER_TIP;
import static cn.acecandy.fasaxi.eva.utils.GameEventUtil.*;

/**
 * 游戏 回调事件
 *
 * @author AceCandy
 * @since 2025/01/10
 */
@Slf4j
@Component
public class GameEvent {

    @Resource
    private EmbyTelegramBot telegramBot;
    @Resource
    private Command command;
    @Resource
    private EmbyDao embyDao;


    public void onClick(Update update, boolean isGroupMessage) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (!JSONUtil.isTypeJSON(callbackQuery.getData())) {
            return;
        }
        JSONObject callbackJn = JSONUtil.parseObj(callbackQuery.getData());
        String action = callbackJn.getStr("action");

        if (isPublicAction(action)) {
            handlePublicAction(action);
        } else {
            if (!isGroupMessage) {
                return;
            }
            handleGroupAction(callbackQuery, callbackJn, action);
        }
    }

    /**
     * 处理群内回调行为
     *
     * @param callbackQuery 回调查询
     * @param callbackJn    回调jn
     * @param action        行动
     */
    private void handleGroupAction(CallbackQuery callbackQuery, JSONObject callbackJn, String action) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Game game = GameListUtil.getGame(chatId);
        if (null == game) {
            return;
        }

        AnswerCallbackQuery callback = new AnswerCallbackQuery(callbackQuery.getId());
        processAction(callbackQuery, game, callback, callbackJn, action);
        telegramBot.sendCallback(callback);
    }

    /**
     * 处理 行为操作
     *
     * @param callbackQuery 回调查询
     * @param game          游戏
     * @param callback      回拨
     * @param callbackJn    回调jn
     * @param action        行动
     */
    private void processAction(CallbackQuery callbackQuery, Game game,
                               AnswerCallbackQuery callback, JSONObject callbackJn, String action) {
        User user = callbackQuery.getFrom();
        Long userId = user.getId();
        Emby embyUser = embyDao.findByTgId(userId);
        if (null == embyUser) {
            callback.setText(NO_EMBY_USER_TIP);
            return;
        }
        GameUser gameUser = game.getMember(userId);
        if (null == gameUser && !ACTION_JOIN_GAME.equals(action)) {
            callback.setText("❌ 当前未加入游戏！");
            return;
        }

        switch (action) {
            case ACTION_JOIN_GAME: {
                handleJoinGame(game, user, callback);
                break;
            }
            case ACTION_READY: {
                handleReadyText(game, gameUser);
                break;
            }
            case ACTION_OPEN: {
                handleOpenText(game, userId, callback);
                break;
            }
            case ACTION_VIEW_WORD: {
                // handleViewWord(game, userId, callback);
                break;
            }
            case ACTION_EXIT: {
                handleExitText(game, userId, callback);
                break;
            }
            case ACTION_VOTE: {
                handleVoteText(game, gameUser, callbackJn.getLong("to"), callback);
                break;
            }
            default:
                break;
        }
    }


    /**
     * 处理公共行为
     *
     * @param action 行动
     */
    private void handlePublicAction(String action) {
        List<String> actionList = StrUtil.splitTrim(action, ":");
        action = CollUtil.getFirst(actionList);

        if (action.equals(PUBLIC_ACTION_RANKS)) {
            if (null == command.rankMsg) {
                return;
            }
            command.handleEditRank(Integer.valueOf(CollUtil.getLast(actionList)));
        }
    }

}