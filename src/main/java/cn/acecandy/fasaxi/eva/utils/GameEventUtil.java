package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.OPERATED_BEFORE;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.notSpeakVote;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.notVoteSelf;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.success;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.MAX_PLAYER;

/**
 * 事件 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class GameEventUtil {
    private GameEventUtil() {
    }

    public final static String ACTION_JOIN_GAME = "joinGame";
    public final static String ACTION_VOTE = "vote";
    public final static String ACTION_READY = "ready";
    public final static String ACTION_OPEN = "open";
    public final static String ACTION_EXIT = "exit";
    public final static String ACTION_VIEW_WORD = "viewWord";

    public final static String PUBLIC_ACTION_RANKS = "public|wodi_ranks";

    public final static String PUBLIC_ACTION = "public|";

    /**
     * 是否公共行为
     *
     * @param action 行动
     * @return boolean
     */
    public static boolean isPublicAction(String action) {
        return StrUtil.startWith(action, PUBLIC_ACTION);
    }

    /**
     * 处理 投票
     *
     * @param game     游戏
     * @param userId   用户id
     * @param voteToId 投票给id
     * @param callback 回拨
     */
    public static void handleVoteText(Game game, Long userId, Long voteToId,
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
            } else {
                callback.setText(OPERATED_BEFORE);
            }
        }
    }

    /**
     * 处理 准备
     *
     * @param game   游戏
     * @param userId 用户id
     */
    public static void handleReadyText(Game game, Long userId) {
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
    public static void handleOpenText(Game game, Long userId, AnswerCallbackQuery callback) {
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
     * 处理 退出游戏
     *
     * @param game     游戏
     * @param userId   用户id
     * @param callback 应答回调查询
     */
    public static void handleExitText(Game game, Long userId, AnswerCallbackQuery callback) {
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
     * 处理 查看词
     *
     * @param game     游戏
     * @param userId   用户id
     * @param callback 应答回调查询
     */
    public static void handleViewWord(Game game, Long userId, AnswerCallbackQuery callback) {
        if (game.getStatus() != GameStatus.讨论时间) {
            return;
        }
        if (game.memberList.stream().noneMatch(m -> m.id.equals(userId))) {
            callback.setText("❌ 未游戏玩家无法查看");
        }
    }


    /**
     * 处理 加入游戏
     *
     * @param game     游戏
     * @param user     用户
     * @param callback 回拨
     */
    public static void handleJoinGame(Game game, User user, AnswerCallbackQuery callback) {
        if (game.getStatus() != GameStatus.等待加入) {
            return;
        }
        if (CollUtil.size(game.memberList) < MAX_PLAYER) {
            game.joinGame(user);
        } else {
            callback.setText("❌ 加入人数已达上限");
        }
    }
}