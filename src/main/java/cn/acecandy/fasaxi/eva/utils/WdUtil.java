package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.bot.game.GameUser;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.entity.WodiTop;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.acecandy.fasaxi.eva.dao.entity.XInvite;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.User;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.*;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.CONTINUOUS_ABSTAINED;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.DiscussionTimeLimit;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.DiscussionTimeLimitMin;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.GameSecondsAddedByThePlayer;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.MAXIMUM_VOTE;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.notVote;

/**
 * 游戏工具类
 * <p>
 * 获取数据
 *
 * @author AceCandy
 * @since 2024/10/17
 */
@Slf4j
public final class WdUtil extends WdSubUtil {
    private WdUtil() {
    }

    /**
     * 出局用户
     * <p>
     * Map<连续弃票, List<GameUser>>
     */
    public final static Map<String, List<GameUser>> OUT_USER = MapUtil.newHashMap();

    /**
     * 获得等级、排名、头衔增益
     *
     * @param level    数量
     * @param wodiTops 水上衣
     * @param top20    排行榜
     * @return double
     */
    public static double getRankBuff(Long tgId, Integer level, List<WodiTop> wodiTops,
                                     List<Map.Entry<Long, Integer>> top20) {
        double buff = 0.05 * level;

        // 战力排名buff
        int rank = findRankIndex(tgId, top20);
        if (rank == 1) {
            buff += 0.25;
        } else if (rank == 2) {
            buff += 0.20;
        } else if (rank == 3) {
            buff += 0.15;
        } else if (rank > 0 && rank <= 10) {
            buff += 0.1;
        } else if (rank > 10 && rank <= 20) {
            buff += 0.05;
        }
        // top头衔buff
        buff += CollUtil.size(wodiTops) * 0.1;
        return buff;
    }

    /**
     * 查询排名位置
     *
     * @param tgId    tg id
     * @param topList 排行榜
     * @return int
     */
    public static int findRankIndex(Long tgId, List<Map.Entry<Long, Integer>> topList) {
        if (CollUtil.isEmpty(topList)) {
            return 0;
        }
        return IntStream.range(0, topList.size())
                .filter(i -> topList.get(i).getKey().equals(tgId))
                .map(i -> i + 1)
                .findFirst().orElse(0);
    }

    public static int findRankIndex(Long tgId, Map<Long, Integer> topMap) {
        if (MapUtil.isEmpty(topMap)) {
            return 0;
        }
        List<Map.Entry<Long, Integer>> topList = CollUtil.newArrayList(topMap.entrySet());
        return findRankIndex(tgId, topList);
    }

    /**
     * 获得等级、排名、头衔增益
     *
     * @param level     数量
     * @param wodiTops  水上衣
     * @param rankIndex 排行榜名次
     * @return double
     */
    public static String getRankBuffStr(Integer level, List<WodiTop> wodiTops,
                                        int rankIndex) {
        StringBuilder sb = new StringBuilder();
        double buff1 = NumberUtil.mul(0.05, level * 1.0);
        if (buff1 > 0) {
            sb.append(StrUtil.format("等级({}) ", buff1));
        }

        // 战力排名buff
        double buff2 = 0;
        if (rankIndex > 0) {
            if (rankIndex == 1) {
                buff2 += 0.25;
            } else if (rankIndex == 2) {
                buff2 += 0.20;
            } else if (rankIndex == 3) {
                buff2 += 0.15;
            } else if (rankIndex <= 10) {
                buff2 += 0.1;
            }
            if (buff2 > 0) {
                sb.append(StrUtil.format("战力({}) ", buff2));
            }
        }

        // top头衔buff
        double buff3 = 0;
        if (CollUtil.isNotEmpty(wodiTops)) {
            buff3 = NumberUtil.mul(0.1, CollUtil.size(wodiTops));
            if (buff3 > 0) {
                sb.append(StrUtil.format("头衔({}) ", buff3));
            }
        }

        return 1 + buff1 + buff2 + buff3 + "【" + sb + "】";
    }

    public static String getRecord(WodiUser user, Emby embyUser, List<WodiTop> wodiTops,
                                   Map<Long, Integer> topMap) {
        Integer completeGame = NumberUtil.nullToZero(user.getCompleteGame());
        Integer wordPeople = NumberUtil.nullToZero(user.getWordPeople());
        Integer wordSpy = NumberUtil.nullToZero(user.getWordSpy());
        Integer wordPeopleVictory = NumberUtil.nullToZero(user.getWordPeopleVictory());
        Integer wordSpyVictory = NumberUtil.nullToZero(user.getWordSpyVictory());
        String recordTxt = RECORD_TXT
                .replace("{userName}", TgUtil.tgNameOnUrl(user))
                .replace("{power}", MapUtil.getStr(topMap, user.getTelegramId(), "0"))
                .replace("{completeGame}", completeGame + "")
                .replace("{word_people}", wordPeople + "")
                .replace("{word_spy}", wordSpy + "")
                .replace("{word_people_victory}", wordPeopleVictory + "")
                .replace("{word_spy_victory}", wordSpyVictory + "")
                .replace("{people_percentage}", NumberUtil.formatPercent(
                        wordPeopleVictory / NumberUtil.toDouble(wordPeople), 1))
                .replace("{spy_percentage}", NumberUtil.formatPercent(
                        wordSpyVictory / NumberUtil.toDouble(wordSpy), 1))
                .replace("{fraction}", user.getFraction() + "")
                .replace("{level}", scoreToTitle(user.getFraction()))
                .replace("{dm}", embyUser.getIv() + "");
        Integer level = scoreToLv(user.getFraction());
        // if (level > 0) {
        // recordTxt = recordTxt.replace("无加成", 1 + 0.1 * level + "倍加成");
        String jiaCheng = getRankBuffStr(
                level, wodiTops, findRankIndex(user.getTelegramId(), topMap));
        if (StrUtil.isNotBlank(jiaCheng)) {
            recordTxt = recordTxt.replace("无加成", jiaCheng);
        }
        if (CollUtil.isNotEmpty(wodiTops)) {
            String title = StrUtil.join("、", wodiTops.stream().map(w ->
                    lvToTitle(w.getLevel()) + "·之王").toList());
            recordTxt = recordTxt.replace("无头衔", title);
        }
        // }
        return recordTxt;
    }

    /**
     * 获取邀请列表
     *
     * @param user     用户
     * @param xInvites x邀请
     * @return {@link String }
     */
    public static String getInviteList(WodiUser user, List<XInvite> xInvites,
                                       Map<Long, WodiUser> embyMap, Map<Long, Integer> ivMap) {
        String recordTxt = INVITE_LIST
                .replace("{userName}", TgUtil.tgNameOnUrl(user))
                .replace("{level}", scoreToTitle(user.getFraction()));
        if (CollUtil.isEmpty(xInvites)) {
            return recordTxt.replace("{list}", "🍂 秋风萧瑟，您的门下还没有传承弟子");
        }
        StringBuilder rankFinal = new StringBuilder();
        xInvites.forEach(x -> {
            WodiUser wdUser = embyMap.get(x.getInviteeId());
            String inviteeName = TgUtil.tgNameOnUrl(wdUser);
            if (StrUtil.isBlank(inviteeName)) {
                inviteeName = x.getInviteeId().toString();
            }
            Integer iv = ivMap.getOrDefault(x.getInviteeId(), 0);
            String inviteeStr = StrUtil.format(INVITE_SINGLE, inviteeName, iv, DateUtil.formatDate(x.getJoinTime()));
            rankFinal.append(inviteeStr);
        });
        return recordTxt.replace("{list}", rankFinal);
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
        String detailSingleFormat = """
                                                总场次:<b>{}</b>
                                                <u>民/卧胜率:<b>{}</b>/ <b>{}</b></u>
                                    """;

        List<List<WodiUser>> users = CollUtil.split(userList, 10);
        userList = CollUtil.get(users, pageNum - 1);
        for (int i = 0; i < userList.size(); i++) {
            WodiUser user = userList.get(i);

            boolean top3 = (pageNum == 1 && i < nos.length);
            String no = top3 ? nos[i] : "🏅";
            String noSingle = StrUtil.format(top3 ? "<b>{}No.{}</b>" : "{}No.{}",
                    no, (pageNum - 1) * 10 + i + 1);

            String rankSingle = StrUtil.format(rankSingleFormat, noSingle,
                    TgUtil.tgNameOnUrl(user), scoreToTitle(user.getFraction()), user.getFraction());
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

    /**
     * 获取排行榜
     *
     * @param top20   Top 20
     * @param userMap 用户映射
     * @return {@link String }
     */
    public static String getRealRank(List<Map.Entry<Long, Integer>> top20,
                                     Map<Long, WodiUser> userMap) {
        StringBuilder rankFinal = new StringBuilder(StrUtil.format(REAL_RANK, CURRENT_SEASON));
        String[] nos = {"👑", "🃏", "🏳️‍🌈"};
        String rankSingleFormat = "{}  | {} | 战力: <b>{}</b> \n";
        // String detailSingleFormat = "      <u>总场次:<b>{}</b>  |  民/卧胜率:<b>{}</b>/ <b>{}</b></u>\n";

        for (int i = 0; i < top20.size(); i++) {
            Map.Entry<Long, Integer> en = top20.get(i);
            Long userId = en.getKey();
            Integer fraction = en.getValue();
            String no = "🎖";
            if (i < 10) {
                no = "🏅";
            }
            if (i < nos.length) {
                no = nos[i];
            }
            String noSingle = StrUtil.format(i < 9 ? "{}No.  {}" : "{}No. {}", no, i + 1);

            WodiUser user = userMap.get(userId);
            String userName = fillWidth(TgUtil.tgName(user), 15);
            String rankSingle = StrUtil.format(rankSingleFormat, noSingle, userName, fraction);

            rankFinal.append(rankSingle);
            if (i == 2 || i == 9) {
                rankFinal.append("┅┅┅┅┅┅┅┅┅┅┅┅\n");
            }
        }
        rankFinal.append(StrUtil.format("\n#WodiRealRank {}", DateUtil.now()));
        return rankFinal.toString();
    }

    public static String getTop(List<WodiTop> topList, Integer season) {
        StringBuilder rankFinal = new StringBuilder(getTopTitle(season != null ? season : CURRENT_SEASON));
        String topSingle = """
                           👑 <b>{}·之王</b>  <i>境内无敌</i> ♦ {} (<i>{}</i>)
                           
                           """;
        topList.forEach(t -> {
            List<String> upTimeList = StrUtil.splitTrim(DateUtil.formatChineseDate(
                    t.getUpTime(), false, true), "日");
            rankFinal.append(StrUtil.format(topSingle, lvToTitle(t.getLevel(), season),
                    TgUtil.tgNameOnUrl(t), CollUtil.getFirst(upTimeList) + "日"));
        });
        rankFinal.append(StrUtil.format("\n#WodiTop {}", DateUtil.now()));
        return rankFinal.toString();
    }

    /**
     * 构建投票结果展示str
     *
     * @param game 游戏
     * @return {@link String }
     */
    public static String buildVotePublicStr(Game game) {
        // 总人数大于6人时，剩余4人时匿名投票
        boolean anonymousVote1 = game.memberList.size() >= 6 && WdUtil.getSurvivesNumber(game) <= 4;
        boolean anonymousVote2 = game.memberList.size() < 6 && WdUtil.getSurvivesNumber(game) <= 3;
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
                        anonymousVote ? "████" : TgUtil.tgNameOnUrl(member.toUser.user)));
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
        stringBuilder.append(StrUtil.join("", finishVoteStr))
                .append(StrUtil.join("", abstainVoteStr))
                .append(StrUtil.join("", notVoteStr)).append("\n");
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
        game.firstSpeakUserId = firstMember.id;
        // 第二名发言指定：从剩下所有人中随机选一个
        GameUser secondMember = RandomUtil.randomEle(game.memberList.stream()
                .filter(m -> m.survive && !m.id.equals(firstMember.id)).toList());
        game.secondSpeakUserId = secondMember.id;
        return StrUtil.format(SPEAK_ORDER, TgUtil.tgNameOnUrl(firstMember), TgUtil.tgNameOnUrl(secondMember));
    }

    /**
     * 处理淘汰成员
     *
     * @return {@link List }<{@link String }>
     */
    public static Map<String, List<GameUser>> execOutMember(Game game, Set<GameUser> memberList) {
        // 本轮淘汰所需票数
        long survivesNumber = WdUtil.getSurvivesNumber(game);
        long weedOut = survivesNumber / 3 + (survivesNumber % 3 > 0 ? 1 : 0);

        List<GameUser> highMembers = WdUtil.getHighestVotedMembers(game);

        Map<String, List<GameUser>> outMap = MapUtil.newHashMap();
        outMap.put("高票", memberList.stream().filter(m ->
                isHighVotedMember(highMembers, m, weedOut)).peek(m -> m.survive = false).toList());
        outMap.put("逃跑", memberList.stream().filter(m ->
                m.survive && m.notVote >= notVote).peek(m -> m.survive = false).toList());
        outMap.put("连续弃票", memberList.stream().filter(m -> m.survive &&
                m.abstainedRound >= CONTINUOUS_ABSTAINED).peek(m -> m.survive = false).toList());
        return outMap;
    }

    /**
     * 是被投票最高的成员
     *
     * @param highMembers 高级成员
     * @param member      成员
     * @param weedOut     淘汰
     * @return boolean
     */
    private static boolean isHighVotedMember(List<GameUser> highMembers,
                                             GameUser member, long weedOut) {
        return CollUtil.size(highMembers) == 1
                && member.beVoted.get() == CollUtil.getFirst(highMembers).beVoted.get()
                && (member.beVoted.get() >= MAXIMUM_VOTE || member.beVoted.get() >= weedOut);
    }

    /**
     * 格式化输出 淘汰名单str
     *
     * @param outMap out地图
     * @return {@link String }
     */
    public static String formatOutStr(Map<String, List<GameUser>> outMap) {
        List<String> strList = CollUtil.newArrayList();
        outMap.forEach((reason, list) -> {
            strList.addAll(list.stream().map(m -> StrUtil.format("{}({}票-{})",
                    TgUtil.tgNameOnUrl(m.user),
                    m.beVoted.get(), reason)).toList());
        });
        return CollUtil.isNotEmpty(strList) ? CollUtil.join(strList, StrUtil.COMMA) : "无";
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
                || (getPeopleSurviveNumber(game) <= 1 && getUndercoverSurvivesNumber(game) >= 0)
                || (getUndercoverNumber(game) == 1 && getUndercoverSurvivesNumber(game) >= 0
                && getSurvivesNumber(game) <= 3);
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
     * 获取平民人数
     *
     * @return int
     */
    public static long getPeopleNumber(Game game) {
        return game.memberList.stream().filter(m -> !m.isUndercover).count();
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

    /**
     * 处于游戏时间(早上10点～晚上10点)
     *
     * @return boolean
     */
    public static boolean isInGameTime() {
        // 获取当前时间
        Date now = DateUtil.date();

        // 获取当天的10:00 AM和10:00 PM
        Date am9 = DateUtil.beginOfDay(now);
        am9 = DateUtil.offset(am9, DateField.HOUR_OF_DAY, 9);
        // 获取当天的10:00 AM和10:00 PM
        Date am12 = DateUtil.beginOfDay(now);
        am12 = DateUtil.offset(am12, DateField.HOUR_OF_DAY, 12);

        // 获取当天的10:00 AM和10:00 PM
        Date pm14 = DateUtil.beginOfDay(now);
        pm14 = DateUtil.offset(pm14, DateField.HOUR_OF_DAY, 14);

        // 获取当天的10:00 AM和10:00 PM
        Date pm18 = DateUtil.beginOfDay(now);
        pm18 = DateUtil.offset(pm18, DateField.HOUR_OF_DAY, 18);

        // 获取当天的10:00 AM和10:00 PM
        Date pm19 = DateUtil.beginOfDay(now);
        pm19 = DateUtil.offset(pm19, DateField.HOUR_OF_DAY, 19);

        Date pm22 = DateUtil.beginOfDay(now);
        pm22 = DateUtil.offset(pm22, DateField.HOUR_OF_DAY, 22);

        // 判断当前时间是否在10:00 AM到10:00 PM之间
        return DateUtil.isIn(now, am9, am12) || DateUtil.isIn(now, pm14, pm18) || DateUtil.isIn(now, pm19, pm22);
    }

    /**
     * 处于禁止通用游戏时间(1:00-3:00 5:00-6:00 7:00-8:00 21:00-23:00)
     *
     * @return boolean
     */
    public static boolean isInNotCommonGameTime() {
        // 获取当前时间
        Date now = DateUtil.date();

        // 获取当天的10:00 AM和10:00 PM
        Date am1 = DateUtil.beginOfDay(now);
        am1 = DateUtil.offset(am1, DateField.HOUR_OF_DAY, 1);
        // 获取当天的10:00 AM和10:00 PM
        Date am2 = DateUtil.beginOfDay(now);
        am2 = DateUtil.offset(am2, DateField.HOUR_OF_DAY, 2)
                .offset(DateField.MINUTE, RandomUtil.randomInt(12, 24));

        // 获取当天的10:00 AM和10:00 PM
        Date pm4 = DateUtil.beginOfDay(now);
        pm4 = DateUtil.offset(pm4, DateField.HOUR_OF_DAY, 4);

        // 获取当天的10:00 AM和10:00 PM
        Date pm6 = DateUtil.beginOfDay(now);
        pm6 = DateUtil.offset(pm6, DateField.HOUR_OF_DAY, 6)
                .offset(DateField.MINUTE, RandomUtil.randomInt(9, 33));

        // 获取当天的10:00 AM和10:00 PM
        Date pm7 = DateUtil.beginOfDay(now);
        pm7 = DateUtil.offset(pm7, DateField.HOUR_OF_DAY, 7);

        Date pm8 = DateUtil.beginOfDay(now);
        pm8 = DateUtil.offset(pm8, DateField.HOUR_OF_DAY, 8)
                .offset(DateField.MINUTE, RandomUtil.randomInt(42, 51));

        // 获取当天的10:00 AM和10:00 PM
        Date pm21 = DateUtil.beginOfDay(now);
        pm21 = DateUtil.offset(pm21, DateField.HOUR_OF_DAY, 21);

        Date pm23 = DateUtil.beginOfDay(now);
        pm23 = DateUtil.offset(pm23, DateField.HOUR_OF_DAY, 23)
                .offset(DateField.MINUTE, RandomUtil.randomInt(31, 38));

        // 判断当前时间是否在10:00 AM到10:00 PM之间
        return DateUtil.isIn(now, am1, am2) || DateUtil.isIn(now, pm4, pm6)
                || DateUtil.isIn(now, pm7, pm8) || DateUtil.isIn(now, pm21, pm23);
    }

    /**
     * 计算字符串的显示宽度（窄字符=1，宽字符=2）
     *
     * @param str 潜水艇用热中子反应堆
     * @return int
     */
    public static int getDisplayWidth(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFC);
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isHighSurrogate(c)) { // 处理代理对（如emoji）
                i++;
            }
            // 判断是否为宽字符（参考Unicode East Asian Width属性）
            boolean isWide = Character.isIdeographic(c)
                    || c >= 0xFF00 && c <= 0xFFEF // 全角符号
                    || c >= 0x2E80 && c <= 0xA4CF // CJK部首、符号
                    || c >= 0xAC00 && c <= 0xD7A3 // 韩文
                    || c >= 0x4E00 && c <= 0x9FFF // 汉字
                    || c >= 0x3000 && c <= 0x303F; // 标点符号
            width += isWide ? 2 : 1;
        }
        return width;
    }

    /**
     * 填充宽度
     *
     * @param str   潜水艇用热中子反应堆
     * @param width 宽度
     * @return {@link String }
     */
    @SneakyThrows
    public static String fillWidth(String str, int width) {
        int displayWidth = str.getBytes("GBK").length;
        int spacesNeeded = width - displayWidth;
        if (spacesNeeded < 0) {
            spacesNeeded = 0;
        }
        return str + StrUtil.repeat(' ', spacesNeeded);
    }

    @SneakyThrows
    public static void main(String[] args) {
        List<String> list = CollUtil.newArrayList();
        list.add("概");
        list.add("𝓵𝓼𝓪𝓪𝓬");
        list.add("☘️舞动心扉🍀");
        list.add("𝓶");
        list.add("𝙅𝙞𝙖𝙔𝙞");
        list.add("空幻");
        list.forEach(l -> {
            // Console.log("原字符串: {}", l.length());
            // try {
            //     Console.log("原字符串: {}", );
            // } catch (UnsupportedEncodingException e) {
            //     throw new RuntimeException(e);
            // }
            // Console.log("原字符串2: {}", StrUtil.length(l));
            // Console.log("原字符串3: {}", getAdaptiveLength(l));
            // Console.log("原字符串3: {}", getVisualWidth(l));
            // Console.log("原字符串3: {}", getAdaptiveLength(l));
            Console.log(fillWidth(l, 20) + "战斗力");
            // Console.log(StrUtil.fillAfter(l, ' ', 20));
        });
        Console.log();
    }
}