package cn.acecandy.fasaxi.eva.bin;

import cn.acecandy.fasaxi.eva.game.Game;
import cn.acecandy.fasaxi.eva.sql.entity.WodiTop;
import cn.acecandy.fasaxi.eva.sql.entity.WodiUser;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

import static cn.acecandy.fasaxi.eva.bin.Constants.CURRENT_SEASON;
import static cn.acecandy.fasaxi.eva.bin.Constants.RANK;
import static cn.acecandy.fasaxi.eva.bin.Constants.RECORD_TXT;

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

    public static String getRecord(WodiUser user) {
        Integer completeGame = user.getCompleteGame();
        Integer wordPeople = user.getWordPeople();
        Integer wordSpy = user.getWordSpy();
        Integer wordPeopleVictory = user.getWordPeopleVictory();
        Integer wordSpyVictory = user.getWordSpyVictory();
        int totalVictory = wordPeopleVictory + wordSpyVictory;
        String recordTxt = RECORD_TXT
                .replace("{userName}", TgUtil.tgNameOnUrl(user))
                .replace("{joinGame}", user.getJoinGame() + "")
                .replace("{completeGame}", completeGame + "")
                .replace("{exitGame}", user.getExitGame() + "")
                .replace("{word_people}", wordPeople + "")
                .replace("{word_spy}", wordSpy + "")
                .replace("{word_people_victory}", wordPeopleVictory + "")
                .replace("{word_spy_victory}", wordSpyVictory + "")
                .replace("{people_percentage}", NumberUtil.formatPercent(
                        wordPeopleVictory / NumberUtil.toDouble(wordPeople), 1))
                .replace("{spy_percentage}", NumberUtil.formatPercent(
                        wordSpyVictory / NumberUtil.toDouble(wordSpy), 1))
                .replace("{total_percentage}", NumberUtil.formatPercent(
                        totalVictory / NumberUtil.toDouble(completeGame), 1))
                .replace("{fraction}", user.getFraction() + "")
                .replace("{level}", levelByScore(user.getFraction()));
        Integer level = level(user.getFraction());
        if (level > 0) {
            recordTxt = recordTxt.replace("无加成", 1 + 0.1 * level + "倍Dmail加成");
        }
        return recordTxt;
    }

    public static String getRank(List<WodiUser> userList, Integer pageNum) {
        StringBuilder rankFinal = new StringBuilder(StrUtil.format(RANK, CURRENT_SEASON));
        String[] nos = {"🥇", "🥈", "🥉"};
        String rankSingle = "{} | {} | <b>{}</b>（{}）\n";
        String detailSingle = "      <u>总场次:<b>{}</b>  |  民/卧胜率:<b>{}</b>/ <b>{}</b></u>\n";

        List<List<WodiUser>> users = CollUtil.split(userList, 10);
        userList = CollUtil.get(users, pageNum - 1);
        for (int i = 0; i < userList.size(); i++) {
            String noSingle = "{}No.{}";
            String no = "🏅";
            if (pageNum == 1) {
                if (i < nos.length) {
                    no = nos[i];
                    noSingle = "<b>{}No.{}</b>";
                }
            }
            WodiUser user = userList.get(i);
            noSingle = StrUtil.format(noSingle, no, (pageNum - 1) * 10 + i + 1);
            rankFinal.append(StrUtil.format(rankSingle, noSingle, TgUtil.tgNameOnUrl(user),
                            levelByScore(user.getFraction()), user.getFraction()))
                    .append(StrUtil.format(detailSingle, user.getCompleteGame(),
                            NumberUtil.formatPercent(user.getWordPeopleVictory()
                                    / NumberUtil.toDouble(user.getWordPeople()), 0),
                            NumberUtil.formatPercent(user.getWordSpyVictory()
                                    / NumberUtil.toDouble(user.getWordSpy()), 0)))
            ;
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
     * 获取存活数
     *
     * @return int
     */
    public static long getSurvivesNumber(Game game) {
        return game.memberList.stream().filter(m -> m.survive).count();
    }

    /**
     * 获取卧底人数
     *
     * @return int
     */
    public static long getUndercoverSurvivesNumber(Game game) {
        return game.memberList.stream().filter(m -> m.survive && m.isUndercover).count();
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
     * 是否全部成员都准备了
     *
     * @param game 游戏
     * @return boolean
     */
    public static boolean isAllMemberReady(Game game) {
        return game.memberList.stream().allMatch(member -> member.ready);
    }

    public static void main(String[] args) {
        List<String> upTimeList = StrUtil.splitTrim(DateUtil.formatChineseDate(
                new Date(), false, true), "分");
        Console.log(upTimeList);
    }
}