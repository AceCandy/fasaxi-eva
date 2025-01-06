package cn.acecandy.fasaxi.eva.game;

import cn.acecandy.fasaxi.eva.service.Command;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.acecandy.fasaxi.eva.bin.GameStatus;
import cn.acecandy.fasaxi.eva.bin.GameUtil;
import cn.acecandy.fasaxi.eva.bin.TgUtil;
import cn.acecandy.fasaxi.eva.bot.impl.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.sql.entity.WodiGroup;
import cn.acecandy.fasaxi.eva.sql.entity.WodiTop;
import cn.acecandy.fasaxi.eva.sql.entity.WodiUser;
import cn.acecandy.fasaxi.eva.sql.entity.WodiWord;
import cn.acecandy.fasaxi.eva.sql.service.EmbyDao;
import cn.acecandy.fasaxi.eva.sql.service.WodiGroupDao;
import cn.acecandy.fasaxi.eva.sql.service.WodiTopDao;
import cn.acecandy.fasaxi.eva.sql.service.WodiUserDao;
import cn.acecandy.fasaxi.eva.sql.service.WodiWordDao;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cn.acecandy.fasaxi.eva.bin.Constants.*;
import static cn.acecandy.fasaxi.eva.bin.GameConstants.*;

/**
 * 游戏
 *
 * @author AceCandy
 * @since 2024/10/21
 */
@Slf4j
public class Game extends Thread {

    @Override
    public int hashCode() {
        return Objects.hash(chatId);
    }

    /**
     * 最后活动时间
     */
    public volatile long endActiveTime;
    public Long chatId;
    public User homeOwner;

    @Getter
    GameStatus status;

    public final Set<Member> memberList = new ConcurrentHashSet<>();

    volatile Message sendInviteMessage;
    volatile Message firstMsg;
    /**
     * 发送邀请时间
     */
    volatile long sendInviteTime = 0;

    Chat chat;
    public boolean run = true;
    volatile boolean updateInvitation = false;
    public int rotate = 0;
    /**
     * 讨论截止时间
     */
    long speechTimeEnd;
    /**
     * 投票截止时间
     */
    long voteTimeEnd;
    /**
     * 即将开始投票提醒
     */
    boolean voteReminder;
    String PEOPLE_WORD;
    String SPY_WORD;

    public EmbyTelegramBot tgBot;

    public WodiGroupDao wodiGroupDao;
    public WodiUserDao wodiUserDao;
    public WodiWordDao wodiWordDao;
    public WodiTopDao wodiTopDao;
    public EmbyDao embyDao;

    Game(WodiGroup group, Message message, User user) {
        tgBot = SpringUtil.getBean(EmbyTelegramBot.class);
        wodiGroupDao = SpringUtil.getBean(WodiGroupDao.class);
        wodiUserDao = SpringUtil.getBean(WodiUserDao.class);
        wodiWordDao = SpringUtil.getBean(WodiWordDao.class);
        wodiTopDao = SpringUtil.getBean(WodiTopDao.class);
        embyDao = SpringUtil.getBean(EmbyDao.class);
        this.endActiveTime = System.currentTimeMillis();
        this.status = GameStatus.等待加入;
        this.chat = message.getChat();
        this.chatId = this.chat.getId();

        homeOwner = user;
        joinGame(user == null ? message.getFrom() : user);
        wodiGroupDao.updateGroupData(chatId, chat.getUserName(), chat.getTitle());
        start();
    }

    public void joinGame(User user) {
        Long tgId = user.getId();
        wodiUserDao.updateUserData(tgId,
                user.getUserName(), user.getFirstName(), user.getLastName());
        if (null != getMember(tgId) || status != GameStatus.等待加入) {
            return;
        }
        memberList.add(new Member(user, wodiUserDao.findByGroupIdIfExist(tgId)));
        wodiUserDao.upJoinGame(tgId);
        updateInvitation = true;
        endActiveTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (status != GameStatus.游戏关闭) {
            try {
                long endTime = System.currentTimeMillis();
                handleWaitingToJoinStatus(endTime);
                handleDiscussionTimeStatus(endTime);
                handleVotingStatus(endTime);
            } catch (Exception e) {
                log.warn("定时任务报错!", e);
            } finally {
                ThreadUtil.safeSleep(500);
            }
        }
        GameList.removeGame(this);
    }

    private void handleWaitingToJoinStatus(long endTime) {
        if (status != GameStatus.等待加入) {
            return;
        }
        if (endTime - sendInviteTime > WaitingYoJoinTimeInterval) {
            sendInvite();
        }
        if (updateInvitation) {
            editInvite();
            updateInvitation = false;
        }
        if (CollUtil.size(memberList) == MAX_PLAYER) {
            if (GameUtil.isAllMemberReady(this)) {
                startDiscussion(true);
            }
        }
        if (endTime - endActiveTime > MaxActiveTime) {
            if (!GameUtil.isAllMemberReady(this)) {
                sendTimeoutShutdownMessage();
                status = GameStatus.游戏关闭;
            }
        }
    }

    private void handleDiscussionTimeStatus(long endTime) {
        if (status != GameStatus.讨论时间) {
            return;
        }

        List<Member> boomList = memberList.stream()
                .filter(m -> m.survive && StrUtil.isNotBlank(m.boom)).toList();
        if (CollUtil.isNotEmpty(boomList)) {
            sendBoom(CollUtil.getFirst(boomList));
        }

        if (endTime > speechTimeEnd || memberList.stream()
                .filter(m -> m.survive).allMatch(m -> m.speak)) {
            transitionToVoting();
        } else if (!voteReminder && endTime > (speechTimeEnd - voteReminderVote)) {
            sendAboutToVote();
        }
    }

    private void sendBoom(Member member) {
        SendMessage sendMessage = new SendMessage(chatId.toString(), StrUtil.format(BOOM_WAITING));
        tgBot.sendMessage(sendMessage, 5 * 1000);
        tgBot.muteGroup(chatId);
        ThreadUtil.safeSleep(4 * 1000);

        String boom = member.boom;
        if (StrUtil.equalsIgnoreCase(boom, PEOPLE_WORD)) {
            sendBoomGameOver();
        } else {
            member.survive = false;
            SendMessage failedMsg = new SendMessage(chatId.toString(),
                    StrUtil.format(BOOM_FAIL, TgUtil.tgNameOnUrl(member)));
            tgBot.sendMessage(failedMsg, 8 * 1000);
        }
        tgBot.unmuteGroup(chatId);
    }


    private void handleVotingStatus(long endTime) {
        if (status != GameStatus.投票中) {
            return;
        }

        boolean isFinishVote = memberList.stream().allMatch(m -> m.survive && m.finishVote);
        if (endTime > voteTimeEnd || isFinishVote) {
            processVoteResult(isFinishVote);
        }
    }

    private void sendTimeoutShutdownMessage() {
        SendMessage sendMessage = new SendMessage(chatId.toString(),
                StrUtil.format(TimeoutShutdown, noReadyMember()));
        tgBot.sendMessage(sendMessage);
    }

    public void startDiscussion(boolean start) {
        if (start) {
            log.warn("定时任务扫描自动开始游戏！");
        } else {
            log.warn("手动开始游戏！");
        }
        TimeInterval timer = DateUtil.timer();
        // status = GameStatus.讨论时间;
        embyDao.upIv(homeOwner.getId(), -10);
        SendMessage sendMessage = new SendMessage(chatId.toString(),
                StrUtil.format(gameStart, TgUtil.tgNameOnUrl(homeOwner)));
        tgBot.sendMessage(sendMessage, 5 * 1000);
        log.info("，耗时1：{}ms", timer.intervalMs());
        initWords();
        log.info("，耗时2：{}ms", timer.intervalMs());
        sendUserWord();
        log.info("，耗时3：{}ms", timer.intervalMs());
        sendSpeechPerform();
        log.info("，耗时4：{}ms", timer.intervalMs());
    }

    private void transitionToVoting() {
        status = GameStatus.投票中;
        sendVote();
    }

    public boolean vote(Long userId, Long toUser) {
        boolean ret = false;
        Member member = getMember(userId);
        if (null == member || !member.survive) {
            return false;
        }
        Member toMember = null;
        if (toUser != -1) {
            toMember = getMember(toUser);
            if (toMember == null || !toMember.survive) {
                return false;
            }
        }
        if (!member.finishVote) {
            member.finishVote = true;
            ret = true;
            if (-1 != toUser) {
                toMember.beVoted.incrementAndGet();
                member.toUser = toMember;
            }
        }
        return ret;
    }

    /**
     * 发送开始讨论
     */
    void sendSpeechPerform() {
        rotate++;
        status = GameStatus.讨论时间;
        voteReminder = false;
        long speechTime = GameSecondsAddedByThePlayer * GameUtil.getSurvivesNumber(this);
        if (speechTime > DiscussionTimeLimit) {
            speechTime = DiscussionTimeLimit;
        }
        if (speechTime < DiscussionTimeLimitMin) {
            speechTime = DiscussionTimeLimitMin;
        }
        String people = "";
        boolean isPin = false;
        if (rotate == 1) {
            List<Member> members = memberList.stream()
                    .filter(m -> m.survive && !m.isSpace)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), l -> {
                        if (l.size() < 2) {
                            return CollUtil.newArrayList(); // 如果没有足够的成员，返回 null
                        }
                        // 随机选择两个不同的索引
                        int firstIndex = RandomUtil.randomInt(l.size());
                        int secondIndex;
                        do {
                            secondIndex = RandomUtil.randomInt(l.size());
                        } while (secondIndex == firstIndex);

                        return List.of(l.get(firstIndex), l.get(secondIndex));
                    }));
            people = StrUtil.format(firstSpeak, TgUtil.tgNameOnUrl(CollUtil.getFirst(members)),
                    TgUtil.tgNameOnUrl(CollUtil.getLast(members)));
            isPin = true;
        }

        speechTimeEnd = System.currentTimeMillis() + (speechTime * 1000);
        SendMessage sendMessage = new SendMessage(this.chatId.toString(),
                StrUtil.format(SPEECH_TIME, getSurvivesUserNames(), speechTime, rotate, people));
        sendMessage.setReplyMarkup(TgUtil.getViewWord(tgBot.getBotUsername()));
        if (isPin) {
            Message msg = tgBot.sendMessage(sendMessage);
            tgBot.pinMsg(msg.getChatId(), msg.getMessageId());
            sendInviteMessage = msg;
            firstMsg = msg;
        } else {
            Message msg = tgBot.sendMessage(sendMessage, 0, GameStatus.讨论时间, this);
            sendInviteMessage = msg;
        }

        sendInviteTime = System.currentTimeMillis();
        memberList.forEach(m -> {
            if (m.survive) {
                m.speak = false;
            }
        });
    }

    void sendInvite() {
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(ResourceUtil.getStream(
                        "static/pic/谁是卧底主图.jpeg"), "谁是卧底主图"))
                .caption(StrUtil.format(GamePlayerWaiting, memberList.size(), getUserNames()))
                .replyMarkup(TgUtil.getJoinGameMarkup(memberList.size() >= minMemberSize, this))
                .parseMode(ParseMode.HTML)
                .build();
        sendInviteMessage = tgBot.sendPhoto(sendPhoto, WaitingYoJoinTimeInterval,
                GameStatus.等待加入, this);
        sendInviteTime = System.currentTimeMillis();
    }

    public void editInvite() {
        if (sendInviteMessage == null) {
            return;
        }
        tgBot.editMessage(sendInviteMessage, StrUtil.format(GamePlayerWaiting, memberList.size(), getUserNames()),
                TgUtil.getJoinGameMarkup(memberList.size() >= minMemberSize, this));
    }

    public Member getMember(@NotNull Long userId) {
        return memberList.stream().filter(m -> m.id.equals(userId)).findFirst().orElse(null);
    }

    /**
     * 是发言成员
     *
     * @param userId 用户id
     * @return boolean
     */
    public boolean isSpeakMember(@NotNull Long userId) {
        Member member = getMember(userId);
        return null != member && member.speak;
    }

    public void exitGame(@NotNull Long userId) {
        memberList.forEach(m -> {
            if (Objects.equals(m.id, userId)) {
                memberList.remove(m);
            }
        });
    }


    /**
     * 未准备成员
     *
     * @return {@link String }
     */
    String noReadyMember() {
        return memberList.stream().filter(m -> !m.ready)
                .map(m -> TgUtil.tgNameOnUrl(m.user))
                .collect(Collectors.joining(StrUtil.COMMA));
    }

    /**
     * 获取用户名
     *
     * @return {@link String }
     */
    public String getUserNames() {
        return memberList.stream().map(m -> {
            String memberStr = TgUtil.tgNameOnUrl(m.user);
            if (m.ready) {
                memberStr = StrUtil.format("<b>{}({})</b>", memberStr, READY);
            }
            return memberStr;
        }).collect(Collectors.joining("、"));
    }

    /**
     * 获取存活用户名单
     *
     * @return {@link String }
     */
    public String getSurvivesUserNames() {
        return memberList.stream().filter(m -> m.survive)
                .map(m -> TgUtil.tgNameOnUrl(m.user)).collect(Collectors.joining("、"));
    }

    /**
     * 发牌
     */
    void initWords() {
        WodiWord word = wodiWordDao.getRandom2();
        if (word == null) {
            return;
        }
        boolean b = RandomUtil.randomBoolean();

        String wordPeople = b ? word.getPeopleWord() : word.getSpyWord();
        String wordSpy = b ? word.getSpyWord() : word.getPeopleWord();
        String wordBlank = " ";

        PEOPLE_WORD = wordPeople;
        SPY_WORD = wordSpy;
        wodiWordDao.upPlayTime(word.getId());

        // 总人数
        int size = memberList.size();

        // 确定卧底数量
        int spyCount = 0;
        // 白板数量
        int blankCount = 0;
        // 特殊模式
        boolean specialMode = false;
        if (size == 4) {
            spyCount = 1;
        } else if (size == 5) {
            if (RandomUtil.randomInt(10) <= 1) {
                // 特殊模式下 白板属于平民 非白板为卧底
                specialMode = true;
            } else {
                blankCount = RandomUtil.randomBoolean() ? 1 : 0;
            }
            spyCount = 1;
        } else if (size == 6) {
            spyCount = 2;
            blankCount = RandomUtil.randomBoolean() ? 1 : 0;
        } else if (size == 7) {
            spyCount = 3;
            blankCount = RandomUtil.randomBoolean() ? 1 : 0;
        } else if (size == 8) {
            spyCount = 3;
            blankCount = 1;
        } else if (size == 9 || size == 10) {
            spyCount = 4;
            blankCount = 1;
        }

        /*if (size >= 4 && size <= 5) {
            spyCount = 1;
        } else if (size >= 6 && size <= 7) {
            spyCount = 2;
        } else if (size >= 8 && size <= 9) {
            spyCount = 3;
            blankCount = 1;
        } else if (size >= 10) {
            spyCount = 4;
            blankCount = 1;
        }*/

        // 分配卧底
        Set<Member> spyMembers = RandomUtil.randomEleSet(memberList, spyCount);
        spyMembers.forEach(m -> {
            m.word = wordSpy;
            m.isUndercover = true;
        });
        // 卧底中选择白板
        RandomUtil.randomEleSet(spyMembers, blankCount).forEach(m -> {
            m.word = wordBlank;
            m.isSpace = true;
        });
        // 分配普通单词
        for (Member m : memberList) {
            if (!m.isUndercover) {
                if (specialMode) {
                    m.word = wordBlank;
                } else {
                    m.word = wordPeople;
                }
            }
        }
    }

    void sendUserWord() {
        memberList.parallelStream().forEach(m -> {
            SendMessage sendMessage = new SendMessage(m.id.toString(),
                    StrUtil.format(sendWord, chat.getTitle(), m.word));
            Message message = tgBot.sendMessage(sendMessage);
        });
    }

    void processVoteResult(boolean isFinishVote) {
        StringBuilder stringBuilder = new StringBuilder();
        if (isFinishVote) {
            stringBuilder.append(everyoneVoted).append("\n");
        } else {
            stringBuilder.append(votedTimeEnd).append("\n");
        }

        boolean anonymousVote = GameUtil.getSurvivesNumber(this) <= 4;
        if (anonymousVote) {
            stringBuilder.append(ANONYMOUS_VOTE).append("\n");
        }
        // 投给谁
        for (Member member : memberList) {
            if (member.survive && member.toUser != null) {
                stringBuilder.append(TgUtil.tgNameOnUrl(member.user))
                        .append(" 👉 [")
                        .append(anonymousVote ? "🀫🀫🀫🀫" : TgUtil.tgNameOnUrl(member.toUser.user))
                        .append("]\n");
                member.notVote = 0;
            }
        }
        // 放弃投
        for (Member member : memberList) {
            if (member.survive && member.toUser == null && member.finishVote) {
                stringBuilder.append(StrUtil.format(ABSTAINED, TgUtil.tgNameOnUrl(member.user)))
                        .append("\n");
                member.notVote = 0;
            }
        }
        // 没有在时间内投票
        for (Member member : memberList) {
            if (member.survive && !member.finishVote) {
                stringBuilder.append(StrUtil.format(NOT_VOTE, TgUtil.tgNameOnUrl(member.user)))
                        .append("\n");
                member.notVote++;
            }
        }
        long survivesNumber = GameUtil.getSurvivesNumber(this);
        // 本轮淘汰所需票数
        long weedOut = survivesNumber / 3 + (survivesNumber % 3 > 0 ? 1 : 0);
        stringBuilder.append("\n").append(StrUtil.format(ELIMINATED_IN_THIS_ROUND, rotate));
        // 淘汰
        List<String> surviveStr = CollUtil.newArrayList();
        List<Member> highMember = getHighestVotedMembers();
        if (CollUtil.size(highMember) == 1) {
            int highVoted = CollUtil.getFirst(highMember).beVoted.get();
            surviveStr.addAll(memberList.stream()
                    .filter(m -> m.survive && m.beVoted.get() == highVoted &&
                            (m.beVoted.get() >= MAXIMUM_VOTE || m.beVoted.get() >= weedOut))
                    .map(m -> {
                        m.survive = false;
                        return TgUtil.tgNameOnUrl(m.user) + StrUtil.format("({}票)", m.beVoted.get());
                    }).toList());
        }
        surviveStr.addAll(memberList.stream().filter(m -> m.survive && m.notVote >= notVote).map(m -> {
                    m.survive = false;
                    return TgUtil.tgNameOnUrl(m.user) + StrUtil.format("({}票-未投票2次)", m.beVoted.get());
                }
        ).toList());

        if (CollUtil.isNotEmpty(surviveStr)) {
            stringBuilder.append(StrUtil.join("、", surviveStr));
        } else {
            stringBuilder.append("无");
        }

        SendMessage sendMessage = new SendMessage(chatId.toString(), stringBuilder.toString());
        tgBot.sendMessage(sendMessage);
        // 判断游戏结束
        if (GameUtil.getUndercoverSurvivesNumber(this) == 0
                || (GameUtil.getPeopleSurviveNumber(this) == 1
                && GameUtil.getUndercoverSurvivesNumber(this) >= 0)) {
            if (GameUtil.getUndercoverSurvivesNumber(this) != 0) {
                // 淘汰剩余平民
                for (Member member : memberList) {
                    if (member.survive && !member.isUndercover) {
                        member.survive = false;
                    }
                }
            }
            sendGameOver();
        } else {
            SendMessage sendMessage1 = new SendMessage(chatId.toString(),
                    StrUtil.format(remainingPersonnel, GameUtil.getSurvivesNumber(this),
                            memberList.size(), getSurvivesUserNames()));
            tgBot.sendMessage(sendMessage1, DiscussionTimeLimit * 1000);
            sendSpeechPerform();
        }
    }

    /**
     * 获得最高票数成员
     *
     * @return {@link List }<{@link Member }>
     */
    public List<Member> getHighestVotedMembers() {
        // 找到最高投票数
        int maxVotes = memberList.stream()
                .filter(m -> m.survive)
                .mapToInt(member -> member.beVoted.get())
                .max()
                .orElse(0);

        // 获取所有投票数等于最高投票数的成员
        return memberList.stream()
                .filter(m -> m.survive && m.beVoted.get() == maxVotes)
                .collect(Collectors.toList());
    }

    void sendBoomGameOver() {
        status = GameStatus.游戏结算中;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(StrUtil.format(GAME_OVER, "🀫 白板")).append(DIVIDING_LINE);

        long undercoverSurviveNum = memberList.stream().filter(m -> m.isUndercover && m.survive).count();
        long surviveNum = memberList.stream().filter(m -> m.survive).count();
        long noSpaceSurviveNum = memberList.stream()
                .filter(m -> m.survive && m.isUndercover && !m.isSpace).count();
        long noSpaceNum = memberList.stream()
                .filter(m -> m.isUndercover && !m.isSpace).count();

        Member member = memberList.stream().filter(m -> m.isSpace).findFirst().get();
        member.fraction = joinScore;
        member.fraction += (8 - memberList.size());
        member.fraction += spaceVictoryScore;
        String homeOwnerFlag = "";
        if (member.id.equals(homeOwner.getId())) {
            homeOwnerFlag = "[房主]";
            member.fraction += 1;
        }
        Integer realFraction = member.fraction;

        String boomStr = "";
        if (surviveNum == 3) {
            member.fraction += 7;
            boomStr += "<b> +7</b>";
            stringBuilder.append(GAME_OVER_BOOM_SPACE3);
        } else {
            if (noSpaceSurviveNum == 0) {
                member.fraction += 4;
                boomStr += "<b> + 4</b>";
                stringBuilder.append(GAME_OVER_BOOM_SPACE);
            }
            if (noSpaceNum == noSpaceSurviveNum) {
                member.fraction -= 1;
                boomStr += "<b> -1</b>";
                stringBuilder.append(GAME_OVER_BOOM_SPACE2);
            }
        }

        stringBuilder.append("\n\n");
        // 其它用户全部杀掉
        memberList.stream().filter(m -> m.survive && !m.isSpace).forEach(m -> m.survive = false);

        stringBuilder.append("🏆 ").append(StrUtil.format(USER_WORD_IS, TgUtil.tgNameOnUrl(member), ""))
                .append(StrUtil.format(" {} +{}", homeOwnerFlag, realFraction))
                .append(boomStr).append("\n");

        stringBuilder.append("\n");
        // 淘汰
        for (Member m : memberList) {
            if (!m.survive) {
                stringBuilder.append("☠️ ").append(StrUtil.format(killUserWordIs,
                        TgUtil.tgNameOnUrl(m.user), m.word));
                m.fraction = m.isUndercover ? spyJoinScore + 1 : joinScore - 1;
                m.fraction += memberList.size() - 8;
                homeOwnerFlag = "";
                if (m.id.equals(homeOwner.getId())) {
                    homeOwnerFlag = "[房主]";
                    m.fraction += 2;
                }
                m.fraction += faileScore;
                stringBuilder.append(m.isUndercover ? "🤡" + homeOwnerFlag + " +" : "👨‍🌾" + homeOwnerFlag + " +")
                        .append(m.fraction).append("\n");
            }
        }
        SendMessage sendMessage = new SendMessage(chatId.toString(), stringBuilder.toString());
        tgBot.sendMessage(sendMessage);

        realSettlement(true);
        status = GameStatus.游戏关闭;
    }

    void sendGameOver() {
        StringBuilder stringBuilder = new StringBuilder();
        status = GameStatus.游戏结算中;

        boolean winnerIsUndercover = memberList.stream().filter(m -> m.survive).anyMatch(m -> m.isUndercover);

        String winner = "";
        if (winnerIsUndercover) {
            winner = "🤡卧底";
        } else {
            winner = "👨‍🌾平民";
        }
        stringBuilder.append(StrUtil.format(GAME_OVER, winner)).append(DIVIDING_LINE);

        long undercoverNum = memberList.stream().filter(m -> m.isUndercover).count();
        long undercoverSurviveNum = memberList.stream().filter(m -> m.isUndercover && m.survive).count();
        long spaceNum = memberList.stream().filter(m -> m.isSpace).count();
        long spaceSurviveNum = memberList.stream().filter(m -> m.isSpace && m.survive).count();
        long peopleNum = memberList.stream().filter(m -> !m.isUndercover).count();
        long peopleSurviveNum = memberList.stream().filter(m -> !m.isUndercover && m.survive).count();

        long surviveNum = memberList.stream().filter(m -> m.survive).count();
        long noSpaceSurviveNum = memberList.stream().filter(m -> m.survive && m.isUndercover && !m.isSpace).count();
        long noSpaceNum = memberList.stream().filter(m -> m.isUndercover && !m.isSpace).count();

        // 如果卧底/民全部存活 积分翻倍
        boolean allUnderCoverSurvive = winnerIsUndercover
                && undercoverNum == surviveNum; //&& undercoverNum > 1;
        boolean allPeopleSurvive = !winnerIsUndercover
                && peopleNum == surviveNum && undercoverNum > 2;
        boolean boom2 = allUnderCoverSurvive || allPeopleSurvive;
        if (allUnderCoverSurvive) {
            stringBuilder.append(GAME_OVER_BOOM_UNDERCOVER);
        } else if (allPeopleSurvive) {
            stringBuilder.append(GAME_OVER_BOOM_PEOPLE);
        }
        boolean allUnderCoverSurviveNoSpace = winnerIsUndercover && spaceNum > 0
                && spaceSurviveNum == 0 && surviveNum == noSpaceNum;
        if (allUnderCoverSurviveNoSpace) {
            stringBuilder.append(GAME_OVER_BOOM_SINGLE_UNDERCOVER);
        }
        boolean singleUnderCoverSurvive = winnerIsUndercover && undercoverNum > 1
                && undercoverSurviveNum == 1 && spaceSurviveNum == 0;
        if (singleUnderCoverSurvive) {
            Member member = CollUtil.getFirst(memberList.stream().filter(m -> m.survive).toList());
            stringBuilder.append(StrUtil.format(GAME_OVER_BOOM_SINGLE_UNDERCOVER2, TgUtil.tgNameOnUrl(member)));
        }

        boolean brotherSurvive = CollUtil.size(memberList) > 4 && peopleSurviveNum == 2;
        if (brotherSurvive) {
            stringBuilder.append(GAME_OVER_BOOM_SINGLE_PEOPLE);
        }

        // 如果白板单独存活 积分翻三倍
        boolean spaceSingleSurvive = memberList.stream().filter(m -> m.survive && m.isSpace).count()
                == memberList.stream().filter(m -> m.survive).count();
        if (spaceSingleSurvive) {
            stringBuilder.append(StrUtil.format(GAME_OVER_BOOM3, getSurvivesUserNames()));
        }

        stringBuilder.append("\n\n");

        for (Member m : memberList) {
            if (m.survive) {
                boolean undercover = m.isUndercover;
                stringBuilder.append("🏆 ");
                stringBuilder.append(StrUtil.format(USER_WORD_IS, TgUtil.tgNameOnUrl(m.user), m.word));
                m.fraction = joinScore;
                if (memberList.size() <= 5) {
                    m.fraction += (memberList.size() - 6);
                }
                String homeOwnerFlag = "";
                if (m.id.equals(homeOwner.getId())) {
                    homeOwnerFlag = "[房主]";
                    m.fraction += 1;
                }
                if (undercover) {
                    m.fraction += spyVictoryScore * (2 + memberList.size() / 8);
                    if (m.isSpace) {
                        m.fraction += spaceVictoryScore;
                    }
                } else {
                    m.fraction += peopleVictoryScore;
                }
                Integer realFraction = m.fraction;
                String boomStr = "";

                if (boom2) {
                    m.fraction *= 2;
                    boomStr += "<b> X 2</b>";
                }
                if (spaceSingleSurvive) {
                    m.fraction *= 3;
                    boomStr += "<b> X 3</b>";
                }
                if (allUnderCoverSurviveNoSpace) {
                    m.fraction += 3;
                    boomStr += "<b> +3</b>";
                }
                if (singleUnderCoverSurvive || brotherSurvive) {
                    m.fraction += 5;
                    boomStr += "<b> +5</b>";
                }

                stringBuilder.append(undercover ? "🤡" + homeOwnerFlag + " +" : "👨‍🌾" + homeOwnerFlag + " +")
                        .append(realFraction).append(boomStr).append("\n");
            }
        }
        stringBuilder.append("\n");
        // 淘汰
        for (Member m : memberList) {
            if (!m.survive) {
                stringBuilder.append("☠️ ");
                stringBuilder.append(StrUtil.format(killUserWordIs,
                        TgUtil.tgNameOnUrl(m.user), m.word));
                m.fraction = m.isUndercover ? spyJoinScore : joinScore;
                if (memberList.size() <= 5) {
                    m.fraction += (memberList.size() - 6);
                }
                String homeOwnerFlag = "";
                if (m.id.equals(homeOwner.getId())) {
                    homeOwnerFlag = "[房主]";
                    m.fraction += 2;
                }
                if ((m.isUndercover && !winnerIsUndercover)
                        || (!m.isUndercover && winnerIsUndercover)) {
                    m.fraction += faileScore;
                }
                stringBuilder.append(m.isUndercover ? "🤡" + homeOwnerFlag + " +" : "👨‍🌾" + homeOwnerFlag + " +")
                        .append(m.fraction).append("\n");
            }
        }
        SendMessage sendMessage = new SendMessage(chatId.toString(), stringBuilder.toString());
        tgBot.sendMessage(sendMessage);

        realSettlement(winnerIsUndercover);
        status = GameStatus.游戏关闭;
    }

    /**
     * 实际结算
     */
    private void realSettlement(boolean winnerIsUndercover) {
        boolean seasonEnds = false;
        try {
            wodiGroupDao.upFinishGame(chatId);
            wodiGroupDao.upMaxOfPeople(chatId, memberList.size());
            List<Long> completeGameId = CollUtil.newArrayList();
            List<Long> wordPeopleId = CollUtil.newArrayList();
            List<Long> wordSpyId = CollUtil.newArrayList();
            List<Long> wordPeopleVictoryId = CollUtil.newArrayList();
            List<Long> wordSpyVictoryId = CollUtil.newArrayList();
            memberList.forEach(m -> {
                Long userId = m.user.getId();
                completeGameId.add(userId);
                if (!m.isUndercover) {
                    wordPeopleId.add(userId);
                } else {
                    wordSpyId.add(userId);
                }
                if (!m.isUndercover && !winnerIsUndercover) {
                    wordPeopleVictoryId.add(userId);
                }
                if (m.isUndercover && winnerIsUndercover) {
                    wordSpyVictoryId.add(userId);
                }
                wodiUserDao.upFraction(userId, m.fraction);
                m.wodiUser.setFraction(m.wodiUser.getFraction() + m.fraction);
            });
            wodiUserDao.upCompleteGame(completeGameId);
            wodiUserDao.upWordPeople(wordPeopleId);
            wodiUserDao.upWordSpy(wordSpyId);
            wodiUserDao.upWordPeopleVictory(wordPeopleVictoryId);
            wodiUserDao.upWordSpyVictory(wordSpyVictoryId);

            // 发币
            StringBuilder mailBuilder = new StringBuilder();
            memberList.forEach(m -> {
                Integer level = GameUtil.level(m.wodiUser.getFraction());
                m.dmailUp = (int) ((m.fraction - 4) * (1 + 0.1 * level));
                mailBuilder.append(StrUtil.format(USER_DMAIL, level, TgUtil.tgNameOnUrl(m.user), m.dmailUp));
                embyDao.upIv(m.user.getId(), m.dmailUp);

                if (m.id.equals(homeOwner.getId())) {
                    String ownerStr = "";
                    if ((m.isUndercover && winnerIsUndercover) || (!m.isUndercover && !winnerIsUndercover)) {
                        m.dmailUp = 12;
                        ownerStr = StrUtil.format(USER_DMAIL_OWNER_WIN,
                                TgUtil.tgNameOnUrl(m.user), m.dmailUp);
                    } else {
                        m.dmailUp = 6;
                        ownerStr = StrUtil.format(USER_DMAIL_OWNER_FAIL,
                                TgUtil.tgNameOnUrl(m.user), m.dmailUp);
                    }
                    mailBuilder.append(ownerStr);
                    embyDao.upIv(m.user.getId(), m.dmailUp);
                }
            });
            int memberSize = memberList.size();
            if (memberSize >= 9) {
                int upRotate = memberSize - 8;
                mailBuilder.append("\n").append(StrUtil.format(USER_FULL, memberSize, upRotate));
                memberList.forEach(m -> embyDao.upIv(m.user.getId(), upRotate));
            }
            if (rotate > memberSize) {
                int upRotate = rotate - memberSize;
                mailBuilder.append("\n").append(StrUtil.format(RORATE_FULL, rotate, upRotate));
                memberList.forEach(m -> embyDao.upIv(m.user.getId(), upRotate));
            }
            tgBot.sendMessage(chatId, mailBuilder.toString());

            StringBuilder upBuilder = new StringBuilder();
            List<Member> upMember = CollUtil.newArrayList();
            memberList.forEach(m -> {
                String oldLevel = m.oldLevel;
                String newLevel = GameUtil.levelByScore(m.wodiUser.getFraction());
                if (!StrUtil.equals(oldLevel, newLevel)) {
                    m.dmailUp = 40 + (GameUtil.level(m.wodiUser.getFraction())) * 10;
                    upBuilder.append(StrUtil.format(USER_LEVEL_UP,
                            TgUtil.tgNameOnUrl(m.user), newLevel, m.dmailUp));
                    embyDao.upIv(m.user.getId(), m.dmailUp);
                    upMember.add(m);
                }
            });
            if (CollUtil.isNotEmpty(upMember)) {
                tgBot.sendMessage(chatId, upBuilder.toString());

                Member maxMember = upMember.stream()
                        .max(Comparator.comparingInt(m -> m.wodiUser.getFraction()))
                        .orElse(null);
                if (null != maxMember) {
                    List<WodiUser> gtF = wodiUserDao.findGtFraction(maxMember.wodiUser.getFraction());
                    if (CollUtil.isEmpty(gtF)) {
                        Integer userScore = maxMember.wodiUser.getFraction();
                        Integer lv = GameUtil.level(maxMember.wodiUser.getFraction());
                        Integer upScore = GameUtil.levelUpScoreByLv(lv);
                        String registerMsg = "";
                        String registerCode = "";
                        if (lv >= 6 && lv <= 9) {
                            registerMsg = StrUtil.format(USER_LEVEL_UP_HIGH, 1);
                            if (lv == 6) {
                                registerCode = RIGISTER_CODE1;
                            } else if (lv == 7) {
                                registerCode = RIGISTER_CODE2;
                            } else if (lv == 8) {
                                registerCode = RIGISTER_CODE3;
                            } else if (lv == 9) {
                                registerCode = RIGISTER_CODE4;
                            }
                        } else if (lv >= 10) {
                            registerMsg = StrUtil.format(USER_LEVEL_UP_HIGH, 1);
                            registerCode = RIGISTER_CODE5;
                        }
                        // maxMember.dmailUp = upScore;
                        String upFirst = StrUtil.format(USER_LEVEL_UP_FIRST,
                                TgUtil.tgNameOnUrl(maxMember.user),
                                GameUtil.levelByLv(lv), upScore, registerMsg);
                        embyDao.allUpIv(5);
                        if (lv >= 10) {
                            upFirst = StrUtil.format(SEASON_ENDS,
                                    TgUtil.tgNameOnUrl(maxMember.user),
                                    GameUtil.levelByLv(lv), upScore, registerMsg);
                            embyDao.allUpIv(5);
                            seasonEnds = true;
                        }
                        embyDao.upIv(maxMember.user.getId(), upScore);


                        if (StrUtil.isNotBlank(registerCode)) {
                            SendMessage sendMessage = new SendMessage(maxMember.id.toString(),
                                    StrUtil.format("您获得了{}: \n{}", USER_LEVEL_UP_HIGH, registerCode));
                            Message message = tgBot.sendMessage(sendMessage);
                        }

                        // 写入碑文
                        WodiTop wodiTop = new WodiTop();
                        BeanUtil.copyProperties(maxMember.wodiUser, wodiTop);
                        wodiTop.setId(null);
                        wodiTop.setLevel(lv);
                        wodiTop.setUpTime(new DateTime());
                        wodiTop.setSeason(CURRENT_SEASON);
                        wodiTopDao.insertOrUpdate(wodiTop);

                        Message msg = tgBot.sendMessage(chatId, upFirst);
                        tgBot.pinMsg(msg.getChatId(), msg.getMessageId());
                    }
                }
            }

        } catch (Exception e) {
            log.error("结算失败，本次成绩不积分：", e);
            SendMessage mailMsg = new SendMessage(chatId.toString(), "结算失败，本次成绩不积分");
            tgBot.sendMessage(mailMsg);
        } finally {
            if (null != firstMsg) {
                // 重置需要发言的条数
                Command.SPEAK_TIME_CNT.set(RandomUtil.randomInt(50, 150));
                if (seasonEnds) {
                    Command.SPEAK_TIME_CNT.set(999);
                }
                tgBot.deleteMessage(firstMsg);
            }
        }
    }

    public void speak(Long userId) {
        if (status == GameStatus.讨论时间) {
            Member member = getMember(userId);
            if (member != null && !member.speak && member.survive) {
                member.speak = true;
            }
        }
    }

    /**
     * 白板爆词
     *
     * @param message 消息
     * @param userId  用户id
     * @param text    文本
     */
    public void boom(Message message, Long userId, String text) {
        if (status == GameStatus.讨论时间) {
            Member member = getMember(userId);
            if (member == null || member.speak || !member.survive || !member.isSpace) {
                return;
            }
            tgBot.deleteMessage(message);
            member.boom = text;
        }
    }

    public void memberReady(Long userId) {
        Game.Member member = getMember(userId);
        if (null != member && !member.ready) {
            member.ready = true;
            endActiveTime = System.currentTimeMillis();
            updateInvitation = true;
        }
    }

    void sendVote() {
        voteTimeEnd = System.currentTimeMillis() + voteTimeLimit;
        for (Member member : memberList) {
            if (member.survive) {
                member.finishVote = false;
                member.beVoted.set(0);
                member.toUser = null;
            }
        }
        SendMessage sendMessage = new SendMessage(chatId.toString(), votingStart);
        sendMessage.setReplyMarkup(TgUtil.getVoteMarkup(this));
        tgBot.sendMessage(sendMessage, 0, GameStatus.投票中, this);
    }

    void sendAboutToVote() {
        voteReminder = true;
        boolean someoneSpeaks = false;
        int speaks = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (Member member : memberList) {
            if (member.survive && member.speak) {
                someoneSpeaks = true;
                break;
            }
        }
        if (someoneSpeaks) {
            stringBuilder.append(StrUtil.format(aboutToVoteL, voteReminderVote / 1000));
            boolean b = false;
            for (Member member : memberList) {
                if (member.survive && !member.speak) {
                    if (b) stringBuilder.append("、");
                    if (!b) b = true;
                    speaks++;
                    stringBuilder.append(TgUtil.tgNameOnUrl(member.user));
                }
            }
            stringBuilder.append(aboutToVoteR);
        } else {
            stringBuilder.append(notAdmin);
        }
        if (someoneSpeaks && speaks == 0) return;
        SendMessage sendMessage = new SendMessage(chatId.toString(), stringBuilder.toString());
        tgBot.sendMessage(sendMessage, voteReminderVote);
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    @ToString
    @EqualsAndHashCode
    public static class Member {
        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        public User user;
        public WodiUser wodiUser;
        public Long id;
        String word;

        public String oldLevel;

        /**
         * 被投票
         */
        public AtomicInteger beVoted = new AtomicInteger(0);
        /**
         * 完成投票
         */
        public boolean finishVote;
        /**
         * 是卧底
         */
        public boolean isUndercover;
        /**
         * 是白板
         */
        public boolean isSpace;
        /**
         * 准备
         */
        public boolean ready = false;
        /**
         * 存活
         */
        public boolean survive = true;
        /**
         * 没有投票
         */
        public int notVote = 0;
        /**
         * 投票给
         */
        Member toUser;
        /**
         * 游戏结算分
         */
        public int fraction = 0;
        public int dmailUp = 0;

        public boolean speak = false;
        public String boom = "";

        public Member(User user, WodiUser wodiUser) {
            this.user = user;
            this.id = user.getId();
            this.wodiUser = wodiUser;
            this.oldLevel = GameUtil.levelByScore(wodiUser.getFraction());
        }
    }
}