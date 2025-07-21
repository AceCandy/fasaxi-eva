package cn.acecandy.fasaxi.eva.bot.game;

import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserDao;
import cn.acecandy.fasaxi.eva.task.impl.TgService;
import cn.acecandy.fasaxi.eva.task.impl.WdService;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.acecandy.fasaxi.eva.utils.GlobalUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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
    private TgService tgService;
    @Resource
    private Command command;
    @Resource
    private WdService wdService;
    @Resource
    private EmbyDao embyDao;
    @Autowired
    private WodiUserDao wodiUserDao;


    public void onClick(CallbackQuery callback) {
        JSONObject callbackJn = JSONUtil.parseObj(callback.getData());
        String action = callbackJn.getStr("action");

        if (isPublicAction(action)) {
            handlePublicAction(callback, action);
        } else {
            if (!TgUtil.isGroupMsg(callback)) {
                return;
            }
            handleGroupAction(callback, callbackJn, action);
        }
    }

    /**
     * 处理群内回调行为
     *
     * @param callbackQuery 回调查询
     * @param callbackJn    回调jn
     * @param action        行动
     */
    private void handleGroupAction(CallbackQuery callbackQuery,
                                   JSONObject callbackJn, String action) {
        Game game = GameListUtil.getGame(callbackQuery.getMessage().getChatId().toString());
        if (null == game) {
            return;
        }

        AnswerCallbackQuery callback = new AnswerCallbackQuery(callbackQuery.getId());
        processAction(callbackQuery, game, callback, callbackJn, action);
        tgService.sendCallback(callback);
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
                if (embyDao.findByTgId(userId) == null || wodiUserDao.findByTgId(userId) == null) {
                    callback.setText("❌ 加入游戏前请先私聊下bot哈～");
                    return;
                }
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
    private void handlePublicAction(CallbackQuery callbackQuery, String action) {
        List<String> actionList = StrUtil.splitTrim(action, ":");
        action = CollUtil.getFirst(actionList);

        if (action.equals(PUBLIC_ACTION_RANKS)) {
            if (null == GlobalUtil.rankMsg) {
                return;
            }
            wdService.handleEditRank(Integer.valueOf(CollUtil.getLast(actionList)));
        } else if (action.equals(PUBLIC_ACTION_SB)) {
            AnswerCallbackQuery callback = new AnswerCallbackQuery(callbackQuery.getId());
            command.handleEditSb(callback, callbackQuery.getFrom());
            tgService.sendCallback(callback);
        }
    }

}