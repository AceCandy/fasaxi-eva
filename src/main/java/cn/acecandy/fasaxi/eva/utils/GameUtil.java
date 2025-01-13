package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.bot.game.GameUser;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.entity.WodiTop;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.ANONYMOUS_VOTE;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.CURRENT_SEASON;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.NOT_VOTE;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.RANK;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.READY;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.RECORD_TXT;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.SPEAK_ORDER;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.VOTE_ABSTAINED;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.VOTE_PUBLICITY;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.DiscussionTimeLimit;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.DiscussionTimeLimitMin;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.GameSecondsAddedByThePlayer;

/**
 * 游戏工具类
 * <p>
 * 获取数据
 *
 * @author AceCandy
 * @since 2024/10/17
 */
@Slf4j
public final class GameUtil extends GameSubUtil {
    private GameUtil() {
    }

    public static String getRecord(WodiUser user, Emby embyUser) {
        Integer completeGame = NumberUtil.nullToZero(user.getCompleteGame());
        Integer wordPeople = NumberUtil.nullToZero(user.getWordPeople());
        Integer wordSpy = NumberUtil.nullToZero(user.getWordSpy());
        Integer wordPeopleVictory = NumberUtil.nullToZero(user.getWordPeopleVictory());
        Integer wordSpyVictory = NumberUtil.nullToZero(user.getWordSpyVictory());
        int totalVictory = wordPeopleVictory + wordSpyVictory;
        String recordTxt = RECORD_TXT
                .replace("{userName}", TgUtil.tgNameOnUrl(user))
                .replace("{completeGame}", completeGame + "")
                .replace("{total_percentage}", NumberUtil.formatPercent(
                        totalVictory / NumberUtil.toDouble(completeGame), 1))
                .replace("{word_people}", wordPeople + "")
                .replace("{word_spy}", wordSpy + "")
                .replace("{word_people_victory}", wordPeopleVictory + "")
                .replace("{word_spy_victory}", wordSpyVictory + "")
                .replace("{people_percentage}", NumberUtil.formatPercent(
                        wordPeopleVictory / NumberUtil.toDouble(wordPeople), 1))
                .replace("{spy_percentage}", NumberUtil.formatPercent(
                        wordSpyVictory / NumberUtil.toDouble(wordSpy), 1))
                .replace("{fraction}", user.getFraction() + "")
                .replace("{level}", levelByScore(user.getFraction()))
                .replace("{dm}", embyUser.getIv() + "");
        Integer level = level(user.getFraction());
        if (level > 0) {
            recordTxt = recordTxt.replace("无加成", 1 + 0.1 * level + "倍加成");
        }
        return recordTxt;
    }

    /**
     * 获取排行榜
     *
     * @param userList 用户列表
     * @param pageNum  书籍页码
     * @return {@link String }
     */
    public static String getRank(List<WodiUser> userList, Integer pageNum) {
        StringBuilder rankFinal = new StringBuilder(StrUtil.format(RANK, CURRENT_SEASON));
        String[] nos = {"🥇", "🥈", "🥉"};
        String rankSingleFormat = "{} | {} | <b>{}</b>（{}）\n";
        String detailSingleFormat = "      <u>总场次:<b>{}</b>  |  民/卧胜率:<b>{}</b>/ <b>{}</b></u>\n";

        List<List<WodiUser>> users = CollUtil.split(userList, 10);
        userList = CollUtil.get(users, pageNum - 1);
        for (int i = 0; i < userList.size(); i++) {
            WodiUser user = userList.get(i);

            boolean top3 = (pageNum == 1 && i < nos.length);
            String no = top3 ? nos[i] : "🏅";
            String noSingle = StrUtil.format(top3 ? "<b>{}No.{}</b>" : "{}No.{}",
                    no, (pageNum - 1) * 10 + i + 1);

            String rankSingle = StrUtil.format(rankSingleFormat, noSingle,
                    TgUtil.tgNameOnUrl(user), levelByScore(user.getFraction()), user.getFraction());
            String detailSingle = StrUtil.format(detailSingleFormat, user.getCompleteGame(),
                    NumberUtil.formatPercent(user.getWordPeopleVictory()
                            / NumberUtil.toDouble(user.getWordPeople()), 0),
                    NumberUtil.formatPercent(user.getWordSpyVictory()
                            / NumberUtil.toDouble(user.getWordSpy()), 0));

            rankFinal.append(rankSingle).append(detailSingle);
        }
        rankFinal.append(StrUtil.format("\n#WodiRank {}", DateUtil.now()));
        return rankFinal.toString();
    }

    public static String getTop(List<WodiTop> topList, Integer season) {
        StringBuilder rankFinal = new StringBuilder(getTopTitle(season != null ? season : CURRENT_SEASON));
        String topSingle = """
                           👑 <b>{}</b> <i>飞升第一人</i> | {}
                                        <b>{}</b>
                           """;


        topList.forEach(t -> {
            List<String> upTimeList = StrUtil.splitTrim(DateUtil.formatChineseDate(
                    t.getUpTime(), false, true), "分");
            rankFinal.append(StrUtil.format(topSingle, levelByLv(t.getLevel()),
                    TgUtil.tgNameOnUrl(t), CollUtil.getFirst(upTimeList) + "分"));
        });
        rankFinal.append(StrUtil.format("\n#WodiTop {}", DateUtil.now()));
        return rankFinal.toString();
    }

    /**
     * 按等级获取首飞第一人币奖励
     *
     * @param lv lv
     * @return {@link Integer }
     */
    public static Integer levelUpScoreByLv(Integer lv) {
        if (CURRENT_SEASON == 1) {
            return levelUpScoreByLv1(lv);
        } else if (CURRENT_SEASON == 2) {
            return levelUpScoreByLv2(lv);
        }
        throw new RuntimeException("未知赛季");
    }


    /**
     * 按分数获取称号
     *
     * @param score 分数
     * @return {@link String }
     */
    public static String levelByScore(Integer score) {
        return levelByLv(level(score));
    }

    public static String levelByLv(Integer lv) {
        if (CURRENT_SEASON == 1) {
            return levelByLv1(lv);
        } else if (CURRENT_SEASON == 2) {
            return levelByLv2(lv);
        }
        throw new RuntimeException("未知赛季");
    }

    public static Integer level(Integer score) {
        if (CURRENT_SEASON == 1) {
            return level1(score);
        } else if (CURRENT_SEASON == 2) {
            return level2(score);
        }
        throw new RuntimeException("未知赛季");
    }

    /**
     * 构建投票结果展示str
     *
     * @param game 游戏
     * @return {@link String }
     */
    public static String buildVotePublicStr(Game game) {
        // 总人数大于6人时，剩余4人时匿名投票
        boolean anonymousVote1 = game.memberList.size() >= 6 && GameUtil.getSurvivesNumber(game) <= 4;
        boolean anonymousVote2 = game.memberList.size() < 6 && GameUtil.getSurvivesNumber(game) <= 3;
        boolean anonymousVote = anonymousVote1 || anonymousVote2;

        StringBuilder stringBuilder = new StringBuilder();
        if (anonymousVote) {
            stringBuilder.append(ANONYMOUS_VOTE).append("\n");
        }

        List<String> finishVoteStr = CollUtil.newArrayList();
        List<String> abstainVoteStr = CollUtil.newArrayList();
        List<String> notVoteStr = CollUtil.newArrayList();
        for (GameUser member : game.memberList) {
            if (!member.survive) {
                continue;
            }
            if (member.toUser != null) {
                // 公示投票
                finishVoteStr.add(StrUtil.format(VOTE_PUBLICITY, TgUtil.tgNameOnUrl(member.user),
                        anonymousVote ? "🀫🀫🀫🀫" : TgUtil.tgNameOnUrl(member.toUser.user)));
                member.notVote = 0;
            } else if (member.finishVote) {
                // 投票弃权
                abstainVoteStr.add(StrUtil.format(VOTE_ABSTAINED, TgUtil.tgNameOnUrl(member.user)));
                member.notVote = 0;
            } else {
                // 没有在时间内投票
                notVoteStr.add(StrUtil.format(NOT_VOTE, TgUtil.tgNameOnUrl(member.user)));
                member.notVote++;
            }
        }
        stringBuilder.append(StrUtil.join("、", finishVoteStr))
                .append(StrUtil.join("、", abstainVoteStr))
                .append(StrUtil.join("、", notVoteStr)).append("\n");
        return stringBuilder.toString();
    }

    /**
     * 构建语发言顺序str
     *
     * @param game 游戏
     * @return {@link String }
     */
    public static String buildSpeechSortStr(Game game) {
        if (game.rotate != 1) {
            return "";
        }
        // 第一名发言指定：从不为space的survive成员中选一个
        GameUser firstMember = RandomUtil.randomEle(game.memberList.stream()
                .filter(m -> m.survive && !m.isSpace).toList());
        // 第二名发言指定：从剩下所有人中随机选一个
        GameUser secondMember = RandomUtil.randomEle(game.memberList.stream()
                .filter(m -> m.survive && !m.id.equals(firstMember.id)).toList());
        return StrUtil.format(SPEAK_ORDER, TgUtil.tgNameOnUrl(firstMember), TgUtil.tgNameOnUrl(secondMember));
    }

    /**
     * 获得最高票数成员
     *
     * @return {@link List }<{@link GameUser }>
     */
    public static List<GameUser> getHighestVotedMembers(Game game) {
        // 找到最高投票数
        int maxVotes = game.memberList.stream()
                .filter(m -> m.survive)
                .mapToInt(member -> member.beVoted.get())
                .max()
                .orElse(0);

        // 获取所有投票数等于最高投票数的成员
        return game.memberList.stream()
                .filter(m -> m.survive && m.beVoted.get() == maxVotes)
                .collect(Collectors.toList());
    }

    /**
     * 获取最后投票成员（记录的投票时间最晚的）
     *
     * @param game 游戏
     * @return {@link GameUser }
     */
    public static GameUser lastVoteMember(Game game) {
        return game.memberList.stream().filter(m -> m.survive)
                .max(Comparator.comparingLong(m -> m.voteTime)).get();
    }

    /**
     * 获取所需讨论时间
     *
     * @param game 游戏
     * @return long
     */
    public static long getSpeechTime(Game game) {
        long speechTime = GameSecondsAddedByThePlayer * getSurvivesNumber(game);
        if (speechTime > DiscussionTimeLimit) {
            speechTime = DiscussionTimeLimit;
        } else if (speechTime < DiscussionTimeLimitMin) {
            speechTime = DiscussionTimeLimitMin;
        }
        return speechTime;
    }

    /**
     * 游戏结束
     * <p>
     * 卧底全部死亡或者平民只剩下一名时卧底存在
     *
     * @param game 游戏
     * @return boolean
     */
    public static boolean isGameOver(Game game) {
        return getUndercoverSurvivesNumber(game) == 0
                || (getPeopleSurviveNumber(game) == 1 && getUndercoverSurvivesNumber(game) >= 0);
    }

    /**
     * 特殊游戏结束
     *
     * @param game 游戏
     * @return boolean
     */
    public static boolean isSpecialGameOver(Game game) {
        return getUndercoverSurvivesNumber(game) == 0
                || (getSurvivesNumber(game) <= 3 && getUndercoverSurvivesNumber(game) >= 0);
    }

    /**
     * 存活人数
     *
     * @return int
     */
    public static long getSurvivesNumber(Game game) {
        return game.memberList.stream().filter(m -> m.survive).count();
    }

    /**
     * 获取卧底存活人数
     *
     * @return int
     */
    public static long getUndercoverSurvivesNumber(Game game) {
        return game.memberList.stream().filter(m -> m.survive && m.isUndercover).count();
    }

    /**
     * 获取卧底人数
     *
     * @return int
     */
    public static long getUndercoverNumber(Game game) {
        return game.memberList.stream().filter(m -> m.isUndercover).count();
    }

    /**
     * 获取平民存活人数
     *
     * @param game 游戏
     * @return long
     */
    public static long getPeopleSurviveNumber(Game game) {
        return game.memberList.stream().filter(m -> m.survive && !m.isUndercover).count();
    }

    /**
     * 获取白板存活人数
     *
     * @param game 游戏
     * @return long
     */
    public static long getSpaceSurviveNumber(Game game) {
        return game.memberList.stream().filter(m -> m.isSpace && m.survive).count();
    }

    /**
     * 获取白板人数
     *
     * @param game 游戏
     * @return long
     */
    public static long getSpaceNumber(Game game) {
        return game.memberList.stream().filter(m -> m.isSpace).count();
    }

    /**
     * 获取非白板卧底人数
     *
     * @param game 游戏
     * @return long
     */
    public static long getNoSpaceNumber(Game game) {
        return game.memberList.stream().filter(m -> m.isUndercover && !m.isSpace).count();
    }

    /**
     * 获取非白板存活人数
     *
     * @param game 游戏
     * @return long
     */
    public static long getNoSpaceSurviveNumber(Game game) {
        return game.memberList.stream().filter(m -> m.survive && m.isUndercover && !m.isSpace).count();
    }

    /**
     * 是否全部成员都准备了
     *
     * @param game 游戏
     * @return boolean
     */
    public static boolean isAllMemberReady(Game game) {
        return game.memberList.stream().allMatch(member -> member.ready);
    }

    /**
     * 卧底胜利
     *
     * @param game 游戏
     * @return boolean
     */
    public static boolean isUndercoverWin(Game game) {
        return game.memberList.stream().filter(m -> m.survive).anyMatch(m -> m.isUndercover);
    }

    /**
     * 获取玩家列表的用户名
     *
     * @return {@link String }
     */
    public static String getWaitingUserNames(Set<GameUser> memberList, User homeOwner) {
        return memberList.stream().map(m -> {
            String memberStr = StrUtil.format("<b>{}</b>", TgUtil.tgNameOnUrl(m.user));
            if (m.ready) {
                memberStr = StrUtil.format("{}({})", TgUtil.tgName(m.user), READY);
            }
            if (m.id.equals(homeOwner.getId())) {
                memberStr = StrUtil.format("{} 🚩", memberStr);
            }
            return memberStr;
        }).collect(Collectors.joining("、"));
    }

    public static void main(String[] args) {
        List<String> upTimeList = StrUtil.splitTrim(DateUtil.formatChineseDate(
                new Date(), false, true), "分");
        Console.log(upTimeList);
    }
}