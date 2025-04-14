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
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.User;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.*;
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
                .replace("{level}", levelByScore(user.getFraction()));
        if (CollUtil.isEmpty(xInvites)) {
            return recordTxt.replace("{list}", "🤡 您的门下还没有传承弟子");
        }
        StringBuilder rankFinal = new StringBuilder();
        xInvites.forEach(x -> {
            WodiUser wdUser = embyMap.get(x.getInviteeId());
            String inviteeName = TgUtil.tgNameOnUrl(wdUser);
            if(StrUtil.isBlank(inviteeName)) {
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
            rankFinal.append(StrUtil.format(topSingle, levelByLv(t.getLevel(), season),
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

    public static String levelByLv(Integer lv, Integer season) {
        if (season == 1) {
            return levelByLv1(lv);
        } else if (season == 2) {
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
                || (getPeopleSurviveNumber(game) == 1 && getUndercoverSurvivesNumber(game) >= 0)
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
     * 有效游戏场次
     *
     * @return long
     */
    public static long effectiveGameFreq() {
        long day = DateUtil.betweenDay(DateUtil.date(), DateUtil.parse("2025-01-14"), true);
        return day * 25;
    }


    /**
     * 搜索海报
     *
     * @param root 根
     * @return {@link Path }
     */
    @SneakyThrows
    public static Path searchPoster(String root) {
        final AtomicInteger counter = new AtomicInteger(0);
        final Path[] result = new Path[1];
        try (Stream<Path> stream = Files.find(Paths.get(root), Integer.MAX_VALUE,
                (p, a) -> a.isRegularFile() && p.endsWith("poster.jpg")).parallel()) {
            stream.forEach(file -> reservoirSampling(file, counter, result));
        }
        return counter.get() == 0 ? null : result[0];
    }

    private static final Lock LOCK = new ReentrantLock();

    private static void reservoirSampling(Path file, AtomicInteger counter, Path[] result) {
        int count = counter.getAndIncrement();
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // 无锁优化：使用CAS减少竞争
        if (count == 0) {
            LOCK.lock();
            try {
                result[0] = file;
            } finally {
                LOCK.unlock();
            }
        } else if (rand.nextInt(count + 1) == 0) {
            LOCK.lock();
            try {
                result[0] = file;
            } finally {
                LOCK.unlock();
            }
        }
    }

    /**
     * 番号 匹配正则
     */
    private static final Pattern FH_PATTERN =
            Pattern.compile("^([A-Z0-9]+[-_][A-Z0-9\\d]{2,})(?:[-_][A-Z]+)?\\b.*");

    /**
     * 提取标准番号
     *
     * @param original 原来
     * @return {@link String }
     */
    public static String standardFhName(String original) {
        if (StrUtil.isBlank(original)) {
            return original;
        }
        return CollUtil.getFirst(ReUtil.findAll(FH_PATTERN, StrUtil.trim(original), 1));
    }

    /**
     * 获取番号名称
     *
     * @param filePath 文件路径
     * @return {@link String }
     */
    public static String getFhName(String filePath) {
        int end = StrUtil.lastIndexOfIgnoreCase(filePath, "/");
        if (end == -1) {
            // Windows兼容
            end = StrUtil.lastIndexOfIgnoreCase(filePath, "\\");
        }

        if (end > 0) {
            int start = StrUtil.lastIndexOfIgnoreCase(filePath, "/", end - 1);
            if (start == -1) {
                start = StrUtil.lastIndexOfIgnoreCase(filePath, "\\", end - 1);
            }
            String fhName = StrUtil.sub(filePath, (start != -1) ? start + 1 : 0, end);
            return standardFhName(fhName);
        }
        return null;
    }

    public static void main(String[] args) {
        String filePath = "/private/tmp/test/104DANDY/104DANDY-818-C 座ったままの男を一切动かさないS字尻振り骑乘位で骨抜きにする美尻キャビンアテンダント/poster.jpg";
        Console.log(getFhName(filePath));
        Console.log(StrUtil.lastIndexOfIgnoreCase(filePath, "/vvvv"));
        Console.log(filePath.lastIndexOf('/'));
    }
}