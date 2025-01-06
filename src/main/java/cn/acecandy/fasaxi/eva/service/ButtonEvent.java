package cn.acecandy.fasaxi.eva.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.acecandy.fasaxi.eva.bin.GameStatus;
import cn.acecandy.fasaxi.eva.utils.GameUtil;
import cn.acecandy.fasaxi.eva.bot.impl.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.game.Game;
import cn.acecandy.fasaxi.eva.game.GameList;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;

import static cn.acecandy.fasaxi.eva.bin.Constants.OPERATED_BEFORE;
import static cn.acecandy.fasaxi.eva.bin.Constants.notSpeakVote;
import static cn.acecandy.fasaxi.eva.bin.Constants.notVoteSelf;
import static cn.acecandy.fasaxi.eva.bin.Constants.success;
import static cn.acecandy.fasaxi.eva.bin.GameConstants.MAX_PLAYER;

@Slf4j
@Component
public class ButtonEvent {

    @Resource
    private EmbyTelegramBot telegramBot;
    @Resource
    private Command command;

    public final static String ACTION_JOIN_GAME = "joinGame";
    public final static String ACTION_VOTE = "vote";
    public final static String ACTION_READY = "ready";
    public final static String ACTION_OPEN = "open";
    public final static String ACTION_EXIT = "exit";
    public final static String ACTION_VIEW_WORD = "viewWord";


    public final static String PUBLIC_ACTION = "public|";
    public final static String PUBLIC_ACTION_RANKS = "public|wodi_ranks";

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
     * 处理群组行为
     *
     * @param callbackQuery 回调查询
     * @param callbackJn    回调jn
     * @param action        行动
     */
    private void handleGroupAction(CallbackQuery callbackQuery, JSONObject callbackJn, String action) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Game game = GameList.getGame(chatId);
        if (null == game) {
            return;
        }

        AnswerCallbackQuery callback = new AnswerCallbackQuery(callbackQuery.getId());
        processAction(callbackQuery, game, callback, callbackJn, action);
        telegramBot.sendCallback(callback);
    }

    /**
     * 处理 操作
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
        Long userId = callbackQuery.getFrom().getId();
        switch (action) {
            case ACTION_JOIN_GAME: {
                handleJoinGame(game, user, callback);
                break;
            }
            case ACTION_READY: {
                handleReady(game, userId);
                break;
            }
            case ACTION_OPEN: {
                handleOpen(game, userId, callback);
                break;
            }
            case ACTION_VIEW_WORD: {
                handleViewWord(game, userId, callback);
                break;
            }
            case ACTION_EXIT: {
                handleExit(game, userId, callback);
                break;
            }
            case ACTION_VOTE: {
                Long voteToId = callbackJn.getLong("to");
                handleVote(game, userId, voteToId, callback);
                break;
            }
            default:
                break;
        }
    }

    /**
     * 处理 加入游戏
     *
     * @param game     游戏
     * @param user     用户
     * @param callback 回拨
     */
    private static void handleJoinGame(Game game, User user, AnswerCallbackQuery callback) {
        if (game.getStatus() != GameStatus.等待加入) {
            return;
        }

        if (CollUtil.size(game.memberList) < MAX_PLAYER) {
            game.joinGame(user);
        } else {
            callback.setText("❌ 加入人数已达上限");
        }
    }

    /**
     * 处理 准备
     *
     * @param game   游戏
     * @param userId 用户id
     */
    private static void handleReady(Game game, Long userId) {
        if (game.getStatus() == GameStatus.等待加入) {
            game.memberReady(userId);
        }
    }

    /**
     * 处理 开始游戏
     *
     * @param game     游戏
     * @param userId   用户id
     * @param callback 应答回调查询
     */
    private static void handleOpen(Game game, Long userId, AnswerCallbackQuery callback) {
        if (game.getStatus() != GameStatus.等待加入) {
            return;
        }

        if (!GameUtil.isAllMemberReady(game)) {
            callback.setText("❌ 还有人未准备好噢");
        } else if (!userId.equals(game.homeOwner.getId())) {
            callback.setText("❌ 只有房主能开局");
        } else {
            game.startDiscussion(false);
        }
    }

    /**
     * 处理 查看词
     *
     * @param game     游戏
     * @param userId   用户id
     * @param callback 应答回调查询
     */
    private static void handleViewWord(Game game, Long userId, AnswerCallbackQuery callback) {
        if (game.getStatus() != GameStatus.讨论时间) {
            return;
        }
        if (game.memberList.stream().noneMatch(m -> m.id.equals(userId))) {
            callback.setText("❌ 未游戏玩家无法查看");
        }
    }

    /**
     * 处理 退出游戏
     *
     * @param game     游戏
     * @param userId   用户id
     * @param callback 应答回调查询
     */
    private static void handleExit(Game game, Long userId, AnswerCallbackQuery callback) {
        if (game.getStatus() != GameStatus.等待加入) {
            return;
        }
        if (userId.equals(game.homeOwner.getId())) {
            callback.setText("❌ 房主不能退出");
        } else {
            game.exitGame(userId);
        }
    }

    /**
     * 处理 投票
     *
     * @param game     游戏
     * @param userId   用户id
     * @param voteToId 投票给id
     * @param callback 回拨
     */
    private static void handleVote(Game game, Long userId, Long voteToId,
                                   AnswerCallbackQuery callback) {
        if (game.getStatus() != GameStatus.投票中) {
            return;
        }

        if (userId.equals(voteToId)) {
            callback.setText(notVoteSelf);
        } else if (!game.isSpeakMember(userId)) {
            callback.setText(notSpeakVote);
        } else {
            if (game.vote(userId, voteToId)) {
                callback.setText(success);
            }else{
                callback.setText(OPERATED_BEFORE);
            }
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

    /**
     * 是否公共行为
     *
     * @param action 行动
     * @return boolean
     */
    private static boolean isPublicAction(String action) {
        return StrUtil.startWith(action, PUBLIC_ACTION);
    }
}