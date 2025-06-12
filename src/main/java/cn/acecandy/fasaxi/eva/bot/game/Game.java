package cn.acecandy.fasaxi.eva.bot.game;

import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.entity.WodiTop;
import cn.acecandy.fasaxi.eva.dao.entity.WodiWord;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiTopDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserLogDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiWordDao;
import cn.acecandy.fasaxi.eva.task.impl.PowerRankService;
import cn.acecandy.fasaxi.eva.task.impl.TgService;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.acecandy.fasaxi.eva.utils.PinYinUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.acecandy.fasaxi.eva.utils.WdUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.*;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.MAX_PLAYER;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.MaxActiveTime;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.MaxWattingTime;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.WaitingYoJoinTimeInterval;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.minMemberSize;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.voteReminderVote;
import static cn.acecandy.fasaxi.eva.common.constants.GameValueConstants.voteTimeLimit;
import static cn.acecandy.fasaxi.eva.utils.GlobalUtil.addSpeakCnt;
import static cn.acecandy.fasaxi.eva.utils.GlobalUtil.setSpeakCnt;
import static cn.acecandy.fasaxi.eva.utils.WdUtil.isSpecialGameOver;

/**
 * 游戏主体
 *
 * @author AceCandy
 * @since 2024/10/21
 */
@Slf4j
public class Game {

    @Override
    public int hashCode() {
        return Objects.hash(chatId);
    }

    /**
     * 最后活动时间
     */
    public volatile long endActiveTime;
    public String chatId;
    public User homeOwner;

    @Setter
    @Getter
    GameStatus status;

    public final Set<GameUser> memberList = new ConcurrentHashSet<>();

    volatile Message sendInviteMessage;
    volatile Message firstMsg;

    public Long firstSpeakUserId;
    public Long secondSpeakUserId;
    /**
     * 发送邀请时间
     */
    volatile long sendInviteTime = 0;

    Chat chat;
    public boolean run = true;
    public volatile boolean updateInvitation = false;
    public int rotate = 0;
    /**
     * 讨论截止时间
     */
    long speechTimeEnd = Long.MAX_VALUE;
    /**
     * 投票截止时间
     */
    long voteTimeEnd = Long.MAX_VALUE;
    /**
     * 即将开始投票提醒
     */
    boolean voteReminder;

    /**
     * 每轮发言记录
     */
    List<String> speakList = CollUtil.newArrayList();
    /**
     * 第一轮发言记录
     */
    public List<String> firstSpeakList = CollUtil.newArrayList();
    String PEOPLE_WORD;
    String SPY_WORD;
    public String SPACE_MEMBER;

    private boolean specialMode = false;

    public TgService tgService;

    public WodiUserDao wodiUserDao;
    public WodiWordDao wodiWordDao;
    public WodiTopDao wodiTopDao;
    public EmbyDao embyDao;
    public WodiUserLogDao wodiUserLogDao;
    public PowerRankService powerRankService;

    private final ScheduledExecutorService scheduler = ThreadUtil.createScheduledExecutor(1);

    public Game(Chat chat, User user) {
        initEnv();
        this.status = GameStatus.等待加入;
        this.chat = chat;
        this.chatId = chat.getId().toString();
        this.homeOwner = user;
        this.specialMode = false;

        joinGame(user);
        startGameLoop();
    }

    private void initEnv() {
        wodiUserDao = SpringUtil.getBean(WodiUserDao.class);
        wodiWordDao = SpringUtil.getBean(WodiWordDao.class);
        wodiTopDao = SpringUtil.getBean(WodiTopDao.class);
        embyDao = SpringUtil.getBean(EmbyDao.class);
        wodiUserLogDao = SpringUtil.getBean(WodiUserLogDao.class);
        tgService = SpringUtil.getBean(TgService.class);
        powerRankService = SpringUtil.getBean(PowerRankService.class);
    }

    public void joinGame(User user) {
        if (status != GameStatus.等待加入) {
            return;
        }
        if (null == embyDao.findByTgId(user.getId())) {
            tgService.sendMsg(chatId, NO_EMBY_USER_TIP, 5 * 1000);
            return;
        }

        Long tgId = user.getId();
        if (null != getMember(tgId)) {
            return;
        }
        // 更新用户信息后添加进游戏
        wodiUserDao.updateUserData(tgId, user.getUserName(), user.getFirstName(), user.getLastName());
        memberList.add(new GameUser(user, wodiUserDao.findByGroupIdIfExist(tgId)));
        updateInvitation = true;
        endActiveTime = System.currentTimeMillis();
    }

    private void startGameLoop() {
        scheduler.scheduleAtFixedRate(this::gameStatusCheck, 0, 250, TimeUnit.MILLISECONDS);
    }

    private synchronized void gameStatusCheck() {
        try {
            long endTime = System.currentTimeMillis();
            handleWaitingToJoinStatus(endTime);
            handleSpeakTimeStatus(endTime);
            handleVotingStatus(endTime);
            checkGameEndCondition(); // 检查游戏是否结束
        } catch (Exception e) {
            log.warn("定时任务报错!", e);
        }
    }

    private void checkGameEndCondition() {
        if (status == GameStatus.游戏关闭) {
            scheduler.shutdown();
            GameListUtil.removeGame(this);
        }
    }

    /**
     * 处理等待加入状态
     *
     * @param endTime 结束时间
     */
    private void handleWaitingToJoinStatus(long endTime) {
        if (status != GameStatus.等待加入) {
            return;
        }
        // 30s发送一次游戏邀请链接
        if (endTime - sendInviteTime > WaitingYoJoinTimeInterval) {
            sendInviteMessage = sendInvite();
            sendInviteTime = System.currentTimeMillis();
        }
        // 有人进入或者退出、准备状态变化 也进行更新
        if (updateInvitation) {
            editInvite();
            updateInvitation = false;
        }

        // 满员如果所有人都准备 游戏自动开始
        if (CollUtil.size(memberList) == MAX_PLAYER && WdUtil.isAllMemberReady(this)) {
            startDiscussion();
        }

        // 60s时间结束但是有人尚未准备
        if (endTime - endActiveTime > MaxActiveTime && !WdUtil.isAllMemberReady(this)) {
            tgService.sendMsg(chatId, StrUtil.format(TimeoutShutdown, noReadyMember()), 60 * 1000);
            status = GameStatus.游戏关闭;
        }
        // 360s时间结束但是有人尚未准备
        if (endTime - endActiveTime > MaxWattingTime) {
            tgService.sendMsg(chatId, WattingTimeoutShutdown);
            status = GameStatus.游戏关闭;
        }
    }

    /**
     * 处理发言时间状态
     *
     * @param endTime 结束时间
     */
    private void handleSpeakTimeStatus(long endTime) {
        if (status != GameStatus.讨论时间) {
            return;
        }
        // 出现白板玩家爆词
        List<GameUser> boomList = memberList.stream()
                .filter(m -> m.survive && StrUtil.isNotBlank(m.boom)).toList();
        if (CollUtil.isNotEmpty(boomList)) {
            sendBoom(CollUtil.getFirst(boomList));
        }
        if (status != GameStatus.讨论时间) {
            return;
        }

        if (endTime > speechTimeEnd || memberList.stream()
                .filter(m -> m.survive).allMatch(m -> m.speak)) {
            // 发言结束进入投票阶段
            transitionToVoting();
        } else if (!voteReminder && endTime > (speechTimeEnd - voteReminderVote)) {
            // 即将开始投票提醒
            sendAboutToVote();
        }
    }

    /**
     * 白板爆词杀人事件
     *
     * @param member 成员
     */
    private void sendBoom(GameUser member) {
        tgService.muteGroup(chatId);
        tgService.sendAnimation(SendAnimation.builder().chatId(chatId)
                .caption(StrUtil.format(BOOM_WAITING))
                .animation(new InputFile(
                        ResourceUtil.getStream("static/pic/moon.gif.mp4"), "moon.gif.mp4"))
                .build(), 10 * 1000);
        ThreadUtil.safeSleep(9 * 1000);

        String boom = member.boom;
        if (StrUtil.equalsIgnoreCase(boom, PEOPLE_WORD)) {
            sendBoomGameOver();
        } else {
            member.survive = false;
            embyDao.upIv(member.id, -2);
            tgService.sendMsg(chatId, StrUtil.format(BOOM_FAIL, TgUtil.tgNameOnUrl(member)));

            execGameOver(new StringBuilder());
        }
        tgService.unmuteGroup(chatId);
    }


    /**
     * 处理投票状态检测
     *
     * @param endTime 结束时间
     */
    private void handleVotingStatus(long endTime) {
        if (status != GameStatus.投票中) {
            return;
        }

        // 存活玩家都投票完成
        boolean isFinishVote = memberList.stream().filter(m -> m.survive)
                .allMatch(m -> m.finishVote);
        if (endTime > voteTimeEnd || isFinishVote) {
            processVoteResult(isFinishVote);
        }
    }

    public void startDiscussion() {
        TimeInterval timer = DateUtil.timer();
        embyDao.upIv(homeOwner.getId(), -10);
        tgService.sendMsg(chatId, StrUtil.format(GAME_START, TgUtil.tgNameOnUrl(homeOwner)), 5 * 1000);
        initWords();
        log.warn("游戏开始！平民词：{}，卧底词：{}，白板：{}, 耗时：{}ms",
                PEOPLE_WORD, SPY_WORD, SPACE_MEMBER, timer.intervalMs());
        sendUserWord();
        log.info("玩家收到词，耗时3：{}ms", timer.intervalMs());
        sendSpeechPerform();
        log.info("发送讨论开始tip，耗时4：{}ms", timer.intervalMs());
    }

    /**
     * 投票开始
     */
    private void transitionToVoting() {
        status = GameStatus.投票中;
        voteTimeEnd = System.currentTimeMillis() + voteTimeLimit;
        memberList.stream().filter(member -> member.survive).forEach(member -> {
            member.voteTime = Long.MAX_VALUE;
            member.finishVote = false;
            member.beVoted.set(0);
            member.toUser = null;
        });
        SendMessage sendMessage = new SendMessage(chatId, VOTING_START);
        sendMessage.setReplyMarkup(TgUtil.getVoteMarkup(this));
        tgService.sendMsg(sendMessage, GameStatus.投票中, this);
        tgService.muteGroup(chatId);
    }

    public boolean vote(Long userId, Long toUser) {
        boolean ret = false;
        GameUser member = getMember(userId);
        if (null == member || !member.survive) {
            return false;
        }
        GameUser toMember = null;
        if (toUser != -1) {
            toMember = getMember(toUser);
            if (toMember == null || !toMember.survive) {
                return false;
            }
        }
        if (!member.finishVote) {
            member.finishVote = true;
            member.voteTime = System.currentTimeMillis();
            ret = true;
            if (-1 != toUser) {
                toMember.beVoted.incrementAndGet();
                member.toUser = toMember;
                // member.abstainedRound = 0;
            } /*else {
                // 弃权场次+1
                member.abstainedRound += 1;
            }*/
        }
        return ret;
    }

    /**
     * 发送开始讨论
     */
    void sendSpeechPerform() {
        status = GameStatus.讨论时间;
        voteReminder = false;
        // 回合数+1
        rotate++;
        memberList.stream().filter(GameUser::isSurvive).forEach(m -> m.round = rotate);

        // 获取投票时间、第一轮指定发言人
        long speechTime = WdUtil.getSpeechTime(this);
        String speechSortStr = WdUtil.buildSpeechSortStr(this);
        boolean isPin = StrUtil.isNotBlank(speechSortStr);

        SendMessage sendMessage = new SendMessage(this.chatId.toString(),
                StrUtil.format(SPEECH_TIME, getSurvivesUserNames(), speechTime, rotate, speechSortStr));
        sendMessage.setReplyMarkup(TgUtil.getViewWord(tgService.getBotUsername()));
        Message msg = tgService.sendMsg(sendMessage);

        // 新一轮发言结束时间以及清空之前的发言状态和发言列表
        speechTimeEnd = System.currentTimeMillis() + (speechTime * 1000);
        memberList.stream().filter(GameUser::isSurvive).forEach(m -> m.speak = false);
        CollUtil.clear(speakList);

        sendInviteMessage = msg;
        // 置顶第一轮发言方便回溯
        if (isPin) {
            tgService.pinMsg(msg.getChatId().toString(), msg.getMessageId());
            firstMsg = msg;
        }
    }

    /**
     * 发送游戏邀请
     */
    Message sendInvite() {
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/s{}/游戏主图.webp", CURRENT_SEASON)), "游戏主图"))
                .caption(StrUtil.format(GAME_WAITING, memberList.size(),
                        WdUtil.getWaitingUserNames(memberList, homeOwner)))
                .replyMarkup(TgUtil.getJoinGameMarkup(memberList.size() >= minMemberSize, this))
                .build();
        return tgService.sendPhoto(sendPhoto, WaitingYoJoinTimeInterval, GameStatus.等待加入, this);
    }

    /**
     * 编辑邀请
     */
    public void editInvite() {
        if (sendInviteMessage == null) {
            return;
        }
        tgService.editMsg(sendInviteMessage, StrUtil.format(
                        GAME_WAITING, memberList.size(), WdUtil.getWaitingUserNames(memberList, homeOwner)),
                TgUtil.getJoinGameMarkup(memberList.size() >= minMemberSize, this));
    }

    /**
     * 获取当前游戏中的玩家
     *
     * @param userId 用户id
     * @return {@link GameUser }
     */
    public GameUser getMember(@NotNull Long userId) {
        return memberList.stream().filter(m -> m.id.equals(userId)).findFirst().orElse(null);
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
        if (size == 4) {
            spyCount = 1;
        } else if (size == 5) {
            if (RandomUtil.randomInt(50) <= 1) {
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

        Set<GameUser> spyMembers = CollUtil.newHashSet();
        boolean specialWd = CollUtil.containsAll(memberList.stream().map(GameUser::getId).toList(),
                ListUtil.of(5496150300L, 7629860778L));
        if (size > 6 && specialWd && RandomUtil.randomInt(1, 11) > 2) {

            Set<GameUser> specialSpies = memberList.stream()
                    .filter(u -> u.id == 5496150300L || u.id == 7629860778L)
                    .peek(m -> {
                        m.word = wordSpy;
                        m.isUndercover = true;
                    })
                    .collect(Collectors.toSet());

            // 计算剩余需要随机选择的卧底数量,从非特殊用户中随机选择剩余的卧底
            int remainingSpyCount = Math.max(0, spyCount - specialSpies.size());
            List<GameUser> availableMembers = memberList.stream()
                    .filter(m -> !specialSpies.contains(m))
                    .collect(Collectors.toList());

            Set<GameUser> regularSpies = RandomUtil.randomEleSet(availableMembers, remainingSpyCount);
            regularSpies.forEach(m -> {
                m.word = wordSpy;
                m.isUndercover = true;
            });

            // 合并所有卧底
            spyMembers.addAll(specialSpies);
            spyMembers.addAll(regularSpies);
        } else {
            // 分配卧底
            spyMembers = RandomUtil.randomEleSet(memberList, spyCount);
            spyMembers.forEach(m -> {
                m.word = wordSpy;
                m.isUndercover = true;
            });
        }

        // 卧底中选择白板
        RandomUtil.randomEleSet(spyMembers, blankCount).forEach(m -> {
            m.word = wordBlank;
            m.isSpace = true;
            SPACE_MEMBER = m.user.getFirstName();
        });
        // 分配普通单词
        for (GameUser m : memberList) {
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
            Message message = tgService.sendMsg(sendMessage);
        });
    }

    /**
     * 处理投票结果
     *
     * @param isFinishVote 是否完成投票
     */
    void processVoteResult(boolean isFinishVote) {
        StringBuilder stringBuilder = new StringBuilder(isFinishVote ? ALL_FINISH_VOTED : TIME_END_VOTED);
        // 最后投票人
        stringBuilder.append(StrUtil.format(LAST_VOTE, TgUtil.tgNameOnUrl(WdUtil.lastVoteMember(this))));
        // 投票结果展示
        stringBuilder.append(WdUtil.buildVotePublicStr(this));

        // 淘汰
        stringBuilder.append(StrUtil.format(ELIMINATED_IN_THIS_ROUND, rotate));
        Map<String, List<GameUser>> outMap = WdUtil.execOutMember(this, memberList);
        outMap.get("逃跑").forEach(m -> {
            embyDao.upIv(m.id, -5);
            tgService.sendMsg(chatId, StrUtil.format(NOT_VOTED_TIP, TgUtil.tgNameOnUrl(m.user)));
        });
        stringBuilder.append(WdUtil.formatOutStr(outMap)).append("\n");

        if (!execGameOver(stringBuilder)) {
            stringBuilder.append(StrUtil.format(SURVIVAL_PERSONNEL, WdUtil.getSurvivesNumber(this),
                    memberList.size(), getSurvivesUserNames()));
            tgService.sendMsg(chatId, stringBuilder.toString());
            sendSpeechPerform();
        }
        tgService.unmuteGroup(chatId);
    }

    /**
     * 判断如果游戏结束则执行结算
     *
     * @param stringBuilder 字符串生成器
     * @return boolean
     */
    private boolean execGameOver(StringBuilder stringBuilder) {
        // 判断游戏结束
        if (specialMode && isSpecialGameOver(this)) {
            tgService.sendMsg(chatId, stringBuilder.toString());
            if (WdUtil.getUndercoverSurvivesNumber(this) != 0) {
                // 卧底胜利时需要淘汰剩余平民
                memberList.stream().filter(member -> member.survive && !member.isUndercover)
                        .forEach(member -> member.survive = false);
            }
            sendSpecialGameOver();
            return true;
        } else if (WdUtil.isGameOver(this)) {
            tgService.sendMsg(chatId, stringBuilder.toString());
            if (WdUtil.getUndercoverSurvivesNumber(this) != 0) {
                // 卧底胜利时需要淘汰剩余平民
                memberList.stream().filter(member -> member.survive && !member.isUndercover)
                        .forEach(member -> member.survive = false);
            }
            sendGameOver();
            return true;
        }
        return false;
    }

    /**
     * 白板爆词 游戏结束 积分计算
     */
    void sendBoomGameOver() {
        status = GameStatus.游戏结算中;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(StrUtil.format(GAME_OVER, "", "🀫 白板")).append(DIVIDING_LINE);

        long surviveNum = WdUtil.getSurvivesNumber(this);
        long noSpaceSurviveNum = WdUtil.getNoSpaceSurviveNumber(this);
        long noSpaceNum = WdUtil.getNoSpaceNumber(this);

        GameUser member = memberList.stream().filter(m -> m.survive && m.isSpace).findFirst().get();
        member.fraction = 7;
        // 小于8人 -2分
        member.fraction += memberList.size() < 8 ? -2 : 0;
        // 每回合+1分
        member.fraction += member.round;
        // 房主
        boolean isOwner = member.id.equals(homeOwner.getId());

        // 构建爆词成就str
        buildBoomAchievementStr(isOwner, member, stringBuilder, surviveNum, noSpaceSurviveNum, noSpaceNum);

        // 其它用户全部杀掉
        memberList.stream().filter(m -> m.survive && !m.isSpace).forEach(m -> m.survive = false);

        stringBuilder.append("\n");
        // 淘汰 一视同仁 卧底均+2 平民均+1
        memberList.stream().filter(m -> !m.survive).forEach(m -> {
            stringBuilder.append("☠️ ").append(StrUtil.format(KILL_USER_WORD_IS,
                    TgUtil.tgNameOnUrl(m.user), m.word));
            m.fraction = m.isUndercover ? 2 : 1;
            boolean isOwner2 = m.id.equals(homeOwner.getId());
            m.fraction += isOwner2 ? 1 : 0;
            // 存活1回合+1分
            m.fraction += (m.round - 1) / 2;

            stringBuilder.append(m.isUndercover ? "🤡 +" : "👨‍🌾 +")
                    .append(m.fraction).append(isOwner2 ? " 🚩" : "").append("\n");
        });


        tgService.sendMsg(chatId, stringBuilder.toString());

        realSettlement(true);
        status = GameStatus.游戏关闭;
    }

    /**
     * 游戏结束 积分计算
     */
    void sendGameOver() {
        status = GameStatus.游戏结算中;

        boolean winnerIsUndercover = WdUtil.isUndercoverWin(this);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(StrUtil.format(GAME_OVER, "", winnerIsUndercover ? "🤡卧底" : "👨‍🌾平民"))
                .append(DIVIDING_LINE);

        long undercoverNum = WdUtil.getUndercoverNumber(this);
        long undercoverSurviveNum = WdUtil.getUndercoverSurvivesNumber(this);
        long spaceNum = WdUtil.getSpaceNumber(this);
        long spaceSurviveNum = WdUtil.getSpaceSurviveNumber(this);
        long peopleSurviveNum = WdUtil.getPeopleSurviveNumber(this);
        long peopleNum = WdUtil.getPeopleNumber(this);
        long surviveNum = WdUtil.getSurvivesNumber(this);
        long noSpaceNum = WdUtil.getNoSpaceNumber(this);

        // 如果卧底全部存活 积分翻倍
        boolean allUnderCoverSurvive = undercoverNum > 1 && undercoverNum == undercoverSurviveNum;
        if (allUnderCoverSurvive) {
            stringBuilder.append(GAME_OVER_BOOM_UNDERCOVER);
        }
        // 如果卧底全部存活 但是白板死亡 积分+3（卧底人数>2）
        boolean allUnderCoverSurviveNoSpace = winnerIsUndercover && spaceNum > 0
                && spaceSurviveNum == 0 && undercoverSurviveNum == noSpaceNum && undercoverNum > 2;
        if (allUnderCoverSurviveNoSpace) {
            stringBuilder.append(GAME_OVER_BOOM_SINGLE_UNDERCOVER);
        }
        // 如果卧底单独存活 积分+5（卧底人数>2）
        boolean singleUnderCoverSurvive = undercoverNum > 2 && undercoverSurviveNum == 1 && spaceSurviveNum == 0;
        if (singleUnderCoverSurvive) {
            stringBuilder.append(GAME_OVER_BOOM_SINGLE_UNDERCOVER2);
        }

        // 如果白板单独存活 积分翻三倍
        boolean spaceSingleSurvive = spaceSurviveNum == surviveNum && undercoverNum > 1;
        if (spaceSingleSurvive) {
            stringBuilder.append(StrUtil.format(GAME_OVER_BOOM3, getSurvivesUserNames()));
        }

        // 平民全部存活 积分1.5倍（卧底人数需＞2）
        boolean allPeopleSurvive = peopleNum == surviveNum && !winnerIsUndercover && undercoverNum > 2;
        if (allPeopleSurvive) {
            stringBuilder.append(GAME_OVER_BOOM_PEOPLE);
        }
        // 平民存活且只存活2人 且总人数>=6 积分+3
        boolean brotherSurvive = CollUtil.size(memberList) >= 6 && peopleSurviveNum == 2;
        if (brotherSurvive) {
            stringBuilder.append(GAME_OVER_BOOM_SINGLE_PEOPLE);
        }
        stringBuilder.append("\n");

        List<String> surviveStr = CollUtil.newArrayList();
        List<String> noSurviveStr = CollUtil.newArrayList();
        for (GameUser m : memberList) {
            StringBuilder sb = new StringBuilder();
            boolean undercover = m.isUndercover;
            boolean isOwner = m.id.equals(homeOwner.getId());

            // 底分
            if (undercover) {
                // 卧底4
                m.fraction = 4;
            } else {
                // 平民3
                m.fraction = 3;
            }
            // 每活2个回合(超过人数回合不算)，积分+1
            if (undercoverNum == 1) {
                m.fraction += Math.min(m.round, memberList.size() - undercoverNum + 1) / 2;
            } else {
                m.fraction += Math.min(m.round - 1, memberList.size() - undercoverNum + 1) / 2;
            }

            if (m.survive) {
                // 加上卧底人数/2的分数（0-2）
                m.fraction += undercoverNum / 2;

                String boomStr = buildAchievementStr(m, allPeopleSurvive, allUnderCoverSurvive,
                        spaceSingleSurvive, allUnderCoverSurviveNoSpace, singleUnderCoverSurvive, brotherSurvive);

                surviveStr.add(sb.append("🏆 ")
                        .append(StrUtil.format(USER_WORD_IS, TgUtil.tgNameOnUrl(m.user), m.word))
                        .append(undercover ? "🤡 +" : "👨‍🌾 +").append(m.fraction)
                        .append(boomStr).append(isOwner ? " 🚩" : "").append("\n").toString());
            } else {
                // 输家阵营-2分
                if (allPeopleSurvive && undercover) {
                    // 民全活成就下 卧底分为1
                    m.fraction = 1;
                } else if ((m.isUndercover && !winnerIsUndercover) || (!m.isUndercover && winnerIsUndercover)) {
                    m.fraction -= 2;
                }
                noSurviveStr.add(sb.append("☠️ ")
                        .append(StrUtil.format(KILL_USER_WORD_IS, TgUtil.tgNameOnUrl(m.user), m.word))
                        .append(undercover ? "🤡 +" : "👨‍🌾 +").append(m.fraction)
                        .append(isOwner ? " 🚩" : "").append("\n").toString());
            }
        }
        // 淘汰
        surviveStr.forEach(stringBuilder::append);
        stringBuilder.append("\n");
        noSurviveStr.forEach(stringBuilder::append);
        tgService.sendMsg(chatId, stringBuilder.toString());

        // 实际结算
        realSettlement(winnerIsUndercover);
        status = GameStatus.游戏关闭;
    }

    /**
     * 特殊游戏结束 积分计算
     */
    void sendSpecialGameOver() {
        status = GameStatus.游戏结算中;

        boolean winnerIsUndercover = WdUtil.isUndercoverWin(this);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(StrUtil.format(GAME_OVER, "<b>👿特殊模式👿 </b>",
                        winnerIsUndercover ? "🤡卧底" : "👨‍🌾平民"))
                .append(DIVIDING_LINE);

        long undercoverNum = WdUtil.getUndercoverNumber(this);
        long peopleSurviveNum = WdUtil.getPeopleSurviveNumber(this);
        long surviveNum = WdUtil.getSurvivesNumber(this);

        // 如果卧底胜利 三倍积分
        if (winnerIsUndercover) {
            stringBuilder.append(GAME_OVER_BOOM3_SPECIAL);
        }

        // 平民全部存活 积分2倍
        boolean allPeopleSurvive = peopleSurviveNum == surviveNum;
        if (allPeopleSurvive) {
            stringBuilder.append(GAME_OVER_BOOM_PEOPLE_SPECIAL);
        }

        stringBuilder.append("\n");

        List<String> surviveStr = CollUtil.newArrayList();
        List<String> noSurviveStr = CollUtil.newArrayList();
        for (GameUser m : memberList) {
            StringBuilder sb = new StringBuilder();
            boolean undercover = m.isUndercover;
            boolean isOwner = m.id.equals(homeOwner.getId());

            // 底分：卧底5 平民3
            m.fraction = undercover ? 5 : 3;
            // 每活2个回合(超过人数回合不算)，积分+1
            m.fraction += Math.min(m.round - 1, memberList.size() - undercoverNum + 1) / 2;

            if (m.survive) {
                // 加上卧底人数/2的分数（0-2）
                m.fraction += undercoverNum / 2;

                String boomStr = buildSpecialAchievementStr(m, allPeopleSurvive, winnerIsUndercover);

                surviveStr.add(sb.append("🏆 ")
                        .append(StrUtil.format(USER_WORD_IS, TgUtil.tgNameOnUrl(m.user), m.word))
                        .append(undercover ? "🤡 +" : "👨‍🌾 +").append(m.fraction)
                        .append(boomStr).append(isOwner ? " 🚩" : "").append("\n").toString());
            } else {
                // 输家阵营-2分
                if ((m.isUndercover && !winnerIsUndercover) || (!m.isUndercover && winnerIsUndercover)) {
                    m.fraction -= 2;
                }

                noSurviveStr.add(sb.append("☠️ ")
                        .append(StrUtil.format(KILL_USER_WORD_IS, TgUtil.tgNameOnUrl(m.user), m.word))
                        .append(undercover ? "🤡 +" : "👨‍🌾 +").append(m.fraction)
                        .append(isOwner ? " 🚩" : "").append("\n").toString());
            }
        }
        // 淘汰
        surviveStr.forEach(stringBuilder::append);
        stringBuilder.append("\n");
        noSurviveStr.forEach(stringBuilder::append);
        tgService.sendMsg(chatId, stringBuilder.toString());

        // 实际结算
        realSettlement(winnerIsUndercover);
        status = GameStatus.游戏关闭;
    }

    /**
     * 构建爆词成就str
     *
     * @param isOwner           是所有者
     * @param member            成员
     * @param stringBuilder     字符串生成器
     * @param surviveNum        生存num
     * @param noSpaceSurviveNum 没有空间生存num
     * @param noSpaceNum        无空格编号
     */
    private void buildBoomAchievementStr(boolean isOwner, GameUser member, StringBuilder stringBuilder,
                                         long surviveNum, long noSpaceSurviveNum, long noSpaceNum) {
        String boomStr = "";
        if (surviveNum == 3) {
            member.fraction += 5;
            boomStr += "<b> +5</b>";
            stringBuilder.append(GAME_OVER_BOOM_SPACE3);
        } else {
            if (noSpaceSurviveNum == 0 && noSpaceNum > 0) {
                member.fraction += 4;
                boomStr += "<b> + 4</b>";
                stringBuilder.append(GAME_OVER_BOOM_SPACE);
            }
            if (noSpaceNum > 0 && noSpaceNum == noSpaceSurviveNum) {
                member.fraction -= 1;
                boomStr += "<b> -1</b>";
                stringBuilder.append(GAME_OVER_BOOM_SPACE2);
            }
        }
        stringBuilder.append("\n\n");
        stringBuilder.append("🏆 ").append(StrUtil.format(USER_WORD_IS, TgUtil.tgNameOnUrl(member), ""))
                .append(StrUtil.format("🀫 +{}", member.fraction))
                .append(boomStr).append(isOwner ? " 🚩" : "").append("\n");
    }

    /**
     * 构造成就str
     *
     * @param m                           m
     * @param allPeopleSurvive            所有人都能活下来
     * @param allUnderCoverSurvive        所有被掩盖人都活了下来
     * @param spaceSingleSurvive          太空单人生存
     * @param allUnderCoverSurviveNoSpace 所有隐藏东西都没有空间
     * @param singleUnderCoverSurvive     单人掩护生存
     * @param brotherSurvive              兄弟幸存
     * @return {@link String }
     */
    private String buildAchievementStr(GameUser m, boolean allPeopleSurvive,
                                       boolean allUnderCoverSurvive,
                                       boolean spaceSingleSurvive, boolean allUnderCoverSurviveNoSpace,
                                       boolean singleUnderCoverSurvive, boolean brotherSurvive) {
        String boomStr = "";
        if (allPeopleSurvive) {
            m.fraction *= 1.5;
            boomStr += "<b> (X1.5)</b>";
        }
        if (allUnderCoverSurvive) {
            m.fraction *= 2;
            boomStr += "<b> (X2)</b>";
        }
        if (spaceSingleSurvive) {
            m.fraction *= 3;
            boomStr += "<b> (X3)</b>";
        }
        if (allUnderCoverSurviveNoSpace) {
            m.fraction += 3;
            boomStr += "<b> (+3)</b>";
        }
        if (brotherSurvive) {
            m.fraction += 3;
            boomStr += "<b> (+3)</b>";
        }
        if (singleUnderCoverSurvive) {
            m.fraction += 5;
            boomStr += "<b> (+5)</b>";
        }
        return boomStr;
    }

    private String buildSpecialAchievementStr(GameUser m, boolean allPeopleSurvive,
                                              boolean winnerIsUndercover) {
        String boomStr = "";
        if (allPeopleSurvive) {
            m.fraction *= 2;
            boomStr += "<b> X 2</b>";
        }
        if (winnerIsUndercover) {
            m.fraction *= 3;
            boomStr += "<b> X 3</b>";
        }
        return boomStr;
    }

    /**
     * 实际结算
     */
    private void realSettlement(boolean winnerIsUndercover) {
        boolean seasonEnds = false;
        try {
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
                boolean isVictory = false;
                if (!m.isUndercover && !winnerIsUndercover) {
                    wordPeopleVictoryId.add(userId);
                    isVictory = true;
                }
                if (m.isUndercover && winnerIsUndercover) {
                    wordSpyVictoryId.add(userId);
                    isVictory = true;
                }
                wodiUserDao.upFraction(userId, m.fraction);
                // 将增加后的积分设置设置到当前变量wodiUser中
                m.wodiUser.setFraction(m.wodiUser.getFraction() + m.fraction);
                // 写入日志
                m.setLogId(wodiUserLogDao.addLog(m.id, m.fraction, isVictory));
            });
            wodiUserDao.upCompleteGame(completeGameId);
            wodiUserDao.upWordPeople(wordPeopleId);
            wodiUserDao.upWordSpy(wordSpyId);
            wodiUserDao.upWordPeopleVictory(wordPeopleVictoryId);
            wodiUserDao.upWordSpyVictory(wordSpyVictoryId);

            List<Map.Entry<Long, Integer>> top20 = powerRankService.findTop20ListByCache();
            // 发币
            StringBuilder mailBuilder = new StringBuilder();
            memberList.forEach(m -> {
                Integer level = WdUtil.scoreToLv(m.wodiUser.getFraction());
                List<WodiTop> wodiTops = wodiTopDao.selectByTgId(m.id);
                double buff = WdUtil.getRankBuff(m.id, level, wodiTops, top20);

                m.dmailUp = (int) ((m.fraction - 4) * (1 + buff));
                mailBuilder.append(StrUtil.format(USER_DMAIL, level, TgUtil.tgNameOnUrl(m.user), m.dmailUp));
                embyDao.upIv(m.user.getId(), m.dmailUp);

                if (m.id.equals(homeOwner.getId())) {
                    String ownerStr = "";
                    if ((m.isUndercover && winnerIsUndercover) || (!m.isUndercover && !winnerIsUndercover)) {
                        m.dmailUp = 14;
                        ownerStr = StrUtil.format(USER_DMAIL_OWNER_WIN,
                                TgUtil.tgNameOnUrl(m.user), m.dmailUp);
                    } else {
                        m.dmailUp = 7;
                        ownerStr = StrUtil.format(USER_DMAIL_OWNER_FAIL,
                                TgUtil.tgNameOnUrl(m.user), m.dmailUp);
                    }
                    mailBuilder.append(ownerStr);
                    embyDao.upIv(m.user.getId(), m.dmailUp);
                }
                wodiUserLogDao.upIvById(m.logId, m.dmailUp);
            });
            int memberSize = memberList.size();
            if (memberSize >= 9) {
                int upRotate = memberSize - 8;
                mailBuilder.append("\n").append(StrUtil.format(USER_FULL, memberSize, upRotate));
                memberList.stream().filter(m -> !m.isRunner)
                        .forEach(m -> embyDao.upIv(m.user.getId(), upRotate));
            }
            if (rotate > memberSize + 1) {
                int upRotate = NumberUtil.min(rotate - memberSize - 1, memberSize / 2);
                mailBuilder.append("\n").append(StrUtil.format(RORATE_FULL, rotate, upRotate));
                memberList.stream().filter(m -> !m.isRunner)
                        .forEach(m -> embyDao.upIv(m.user.getId(), upRotate));
            }
            tgService.sendMsg(chatId, mailBuilder.toString());

            StringBuilder upBuilder = new StringBuilder();
            List<GameUser> upMember = CollUtil.newArrayList();
            memberList.forEach(m -> {
                String oldLevel = m.oldLevel;
                String newLevel = WdUtil.scoreToTitle(m.wodiUser.getFraction());
                if (!StrUtil.equals(oldLevel, newLevel)) {
                    m.dmailUp = 90 + WdUtil.scoreToLv(m.wodiUser.getFraction()) * 10;
                    upBuilder.append(StrUtil.format(USER_LEVEL_UP,
                            TgUtil.tgNameOnUrl(m.user), newLevel, m.dmailUp));
                    embyDao.upIv(m.user.getId(), m.dmailUp);
                    wodiUserLogDao.upTopIvById(m.logId, m.dmailUp);
                    upMember.add(m);
                }
            });
            if (CollUtil.isNotEmpty(upMember)) {
                tgService.sendMsg(chatId, upBuilder.toString());
            }
        } catch (Exception e) {
            log.error("游戏检测发生异常，本次成绩不计分：", e);
            SendMessage mailMsg = new SendMessage(chatId, "游戏检测发生异常，本次成绩不记分");
            tgService.sendMsg(mailMsg);
        } finally {
            if (null != firstMsg) {
                setSpeakCnt(50, 80);
                // 重置需要发言的条数
                if (memberList.size() < 6) {
                    addSpeakCnt(20, 40);
                } else if (memberList.size() < 8) {
                    addSpeakCnt(20, 40);
                }
                if (rotate < memberList.size() - 2) {
                    addSpeakCnt(20, 40);
                }
                tgService.unPinMsg(firstMsg.getChatId().toString(), firstMsg.getMessageId());
            }
        }
    }

    /**
     * 玩家发言
     *
     * @param text 文本
     */
    public void speak(Message message, String text) {
        if (status != GameStatus.讨论时间) {
            return;
        }
        Long userId = message.getFrom().getId();
        GameUser member = getMember(userId);
        if (member == null || member.speak || !member.survive) {
            return;
        }
        if (rotate == 1) {
            if (CollUtil.size(speakList) == 0) {
                if (!userId.equals(firstSpeakUserId)) {
                    tgService.sendMsg(chatId, StrUtil.format(RUN_AWAY_QUICKLY, TgUtil.tgNameOnUrl(member)));
                    embyDao.upIv(userId, -5);
                    return;
                }
            } else if (CollUtil.size(speakList) == 1) {
                if (!userId.equals(secondSpeakUserId)) {
                    tgService.sendMsg(chatId, StrUtil.format(RUN_AWAY_QUICKLY, TgUtil.tgNameOnUrl(member)));
                    embyDao.upIv(userId, -5);
                    return;
                }
            }
        }

        text = StrUtil.cleanBlank(text);
        text = StrUtil.removeAny(StrUtil.cleanBlank(text),
                ",", "，", ".", "。", "!", "！", ";", "“", "”");
        String finalText = text;
        if (StrUtil.isBlank(text) || CollUtil.contains(speakList, text)
                || speakList.stream().anyMatch(s -> StrUtil.containsIgnoreCase(s, finalText))) {
            // 发言为空或重复词语
            tgService.sendMsg(chatId, StrUtil.format(SPEAK_REPEAT, TgUtil.tgNameOnUrl(member)));
            embyDao.upIv(userId, -2);
            return;
        }
        String pinyinFirst = PinYinUtil.getFirstLetters(text);
        String pinyin = PinYinUtil.getPingYin(text);
        String wordPinyinFirst = PinYinUtil.getFirstLetters(member.word);
        String wordPinyin = PinYinUtil.getPingYin(member.word);
        if (StrUtil.containsIgnoreCase(text, member.word)) {
            // 违禁爆词 全称
            Emby emby = embyDao.findByTgId(member.id);
            Integer currentIv = emby.getIv();
            if (currentIv < 0) {
                currentIv = 20;
            } else {
                currentIv /= 10;
                if (currentIv < 20) {
                    currentIv = 20;
                }
            }
            tgService.sendAnimation(SendAnimation.builder().chatId(chatId)
                    .replyToMessageId(message.getMessageId())
                    .caption(StrUtil.format(SPEAK_NOWAY_BIG, TgUtil.tgNameOnUrl(member), currentIv))
                    .animation(new InputFile(
                            ResourceUtil.getStream("static/pic/核爆.gif"), "hebao.gif"))
                    .build());
            embyDao.upIv(userId, -currentIv);
        } else if (StrUtil.containsIgnoreCase(member.word, text)
                || StrUtil.containsIgnoreCase(text, wordPinyinFirst)
                || StrUtil.containsIgnoreCase(text, wordPinyin)
                || StrUtil.containsIgnoreCase(wordPinyin, text)
                || StrUtil.containsIgnoreCase(wordPinyinFirst, text)
                || StrUtil.equalsIgnoreCase(pinyin, wordPinyin)
                || PinYinUtil.findAllChar(text, member.word)
                || PinYinUtil.findTwoChar(text, member.word)
        ) {
            // 违禁爆词 本词或者拼音
            tgService.sendMsg(userId.toString(), StrUtil.format(SPEAK_NOWAY, TgUtil.tgNameOnUrl(member)));
            embyDao.upIv(userId, -5);
            log.warn("用户 {} 违禁爆词：{}，自己收到的词: {}", member.user.getFirstName(), text, member.word);
        }
        member.speak = true;
        speakList.add(text);
        if (rotate == 1) {
            firstSpeakList.add(text);
        }
    }

    /**
     * 白板爆词
     *
     * @param message 消息
     * @param text    文本
     */
    public void boom(Message message, String text) {
        if (status != GameStatus.讨论时间) {
            return;
        }
        GameUser member = getMember(message.getFrom().getId());
        if (member == null || member.speak || !member.survive || !member.isSpace) {
            return;
        }
        tgService.delMsg(message);
        member.boom = text;
    }

    /**
     * 即将开始投票
     */
    void sendAboutToVote() {
        voteReminder = true;
        int speaks = 0;
        StringBuilder stringBuilder = new StringBuilder();
        // 有人发言
        boolean someoneSpeaks = memberList.stream().anyMatch(member -> member.survive && member.speak);
        if (someoneSpeaks) {
            stringBuilder.append(StrUtil.format(VOTE_COUNT_DOWN, voteReminderVote / 1000));
            boolean b = false;
            for (GameUser member : memberList) {
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
        SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
        tgService.sendMsg(sendMessage, voteReminderVote);
    }

    public static void main(String[] args) {

        String text = "7";
        String word = "坐忘道";
        String pinyin = PinYinUtil.getFirstLetters(text);
        Console.log(pinyin);
        String wordPinyin = PinYinUtil.getFirstLetters(word);
        Console.log(wordPinyin);
        Console.log(StrUtil.containsAnyIgnoreCase(text, word, pinyin));
        Console.log(StrUtil.containsIgnoreCase("7", "ZWD"));
        Console.log(StrUtil.containsIgnoreCase("7", "坐忘道"));
        Console.log(StrUtil.equalsIgnoreCase(pinyin, wordPinyin));
    }
}