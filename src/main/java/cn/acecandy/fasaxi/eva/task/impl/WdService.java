package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.entity.WodiTop;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUserLog;
import cn.acecandy.fasaxi.eva.dao.entity.XInvite;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiTopDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserLogDao;
import cn.acecandy.fasaxi.eva.dao.service.XInviteDao;
import cn.acecandy.fasaxi.eva.utils.ChineseUtil;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.acecandy.fasaxi.eva.utils.GlobalUtil;
import cn.acecandy.fasaxi.eva.utils.MsgDelUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.acecandy.fasaxi.eva.utils.WdUtil;
import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.*;
import static cn.acecandy.fasaxi.eva.common.enums.GameStatus.讨论时间;
import static cn.acecandy.fasaxi.eva.utils.GlobalUtil.GAME_SPEAK_CNT;
import static cn.acecandy.fasaxi.eva.utils.GlobalUtil.RANK_CACHE;
import static cn.hutool.core.text.CharSequenceUtil.EMPTY;

/**
 * 卧底游戏 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class WdService {

    @Resource
    private TgService tgService;
    @Resource
    private WodiUserDao wodiUserDao;
    @Resource
    private WodiTopDao wodiTopDao;
    @Resource
    private XInviteDao xInviteDao;
    @Resource
    private WodiUserLogDao wodiUserLogDao;
    @Resource
    private PowerRankService powerRankService;

    private final static String 新建游戏 = "/wd";
    private final static String 信息签到 = "/wd_checkin";
    // private final static String 个人信息 = "/wd_info";
    private final static String 积分排行 = "/wd_rank";
    private final static String 战力排行 = "/wd_real_rank";
    private final static String 大佬榜单 = "/wd_top";
    private final static String 关闭游戏 = "/wd_exit";
    private final static String 游戏帮助 = "/wd_help";

    private final static TimedCache<Long, Integer> CACHE_CHECKIN
            = CacheUtil.newTimedCache(4 * 60 * 1000);

    @Resource
    private EmbyDao embyDao;

    public void process(String cmd, Message message) {
        String chatId = message.getChatId().toString();
        Long userId = message.getFrom().getId();
        if (!TgUtil.isGroupMsg(message) && !CollUtil.contains(tgService.getAdmins(), userId)) {
            tgService.sendMsg(chatId, TIP_IN_GROUP, 10 * 1000);
            return;
        }
        switch (cmd) {
            case 新建游戏 -> handleNewGameCommand(message.getFrom(), message.getChat());
            case 信息签到 -> handleCheckinCommand(chatId, userId);
            // case 个人信息 -> handleRecordCommand(chatId, userId);
            case 积分排行 -> handleRankCommand(chatId, userId);
            case 战力排行 -> handleRealRankCommand(chatId, userId);
            case 大佬榜单 -> handleTopCommand(chatId, userId, message.getText());
            case 关闭游戏 -> handleExitCommand(message, chatId, userId);
            case 游戏帮助 -> tgService.sendMsg(chatId, TIP_HELP, 300 * 1000);
            default -> {
            }
        }
    }

    /**
     * 处理 信息签到
     *
     * @param chatId 聊天id
     * @param userId 用户id
     */
    public void handleCheckinCommand(String chatId, Long userId) {
        WodiUser user = wodiUserDao.findByTgId(userId);
        Emby embyUser = embyDao.findByTgId(userId);
        if (user == null || embyUser == null) {
            tgService.sendMsg(chatId, "您还未参与过游戏或者未在bot处登记哦~", 5 * 1000);
            return;
        }
        if (CACHE_CHECKIN.containsKey(userId) && CACHE_CHECKIN.get(userId) == 1) {
            tgService.sendMsg(chatId, TIP_IN_CHECKIN_NOPAY, 2 * 1000);
        } else {
            Integer costIv = 5;
            if (null == embyUser.getIv() || embyUser.getIv() < costIv) {
                tgService.sendMsg(chatId, "您的Dmail不足，无法查看个人信息", 5 * 1000);
                return;
            }
            if (!CollUtil.contains(tgService.getAdmins(), userId)) {
                embyDao.upIv(userId, -costIv);
            }
            tgService.sendMsg(chatId, StrUtil.format(TIP_IN_CHECKIN, costIv), 2 * 1000);
        }
        try {
            ChatMember chatMember = tgService.getChatMember(chatId, userId);
            boolean isAdmin = TgUtil.isAdmin(chatMember);

            List<WodiTop> wodiTops = wodiTopDao.selectByTgId(userId);
            Map<Long, Integer> topMap = powerRankService.findTopByCache();

            // 查询弟子名单 小于21天为未出师弟子（计算22天是为了取昨日）
            List<XInvite> xInvites = xInviteDao.findInviteeByInviter(userId);
            xInvites = xInvites.stream().filter(x ->
                    DateUtil.betweenDay(DateUtil.date(), x.getJoinTime(), false) < 22).toList();
            // 获取昨日弟子表现
            List<Long> yesInviteeIds = xInvites.stream().filter(x -> x.getCollectTime() == null
                            || DateUtil.compare(x.getCollectTime(), DateUtil.yesterday()) < 0)
                    .map(XInvite::getInviteeId).toList();
            Map<Long, Integer> ivMap = wodiUserLogDao.findByTgIdYesterday(yesInviteeIds).stream()
                    .collect(Collectors.groupingBy(WodiUserLog::getTelegramId,
                            Collectors.summingInt(bean ->
                                    (int) (bean.getFraction() * 0.3 + bean.getTiv() * 0.1))
                    ));
            // 今日累计展示
            List<Long> inviteeIds = xInvites.stream().map(XInvite::getInviteeId).toList();
            Map<Long, WodiUser> embyMap = wodiUserDao.findByTgId(inviteeIds).stream().collect(
                    Collectors.toMap(WodiUser::getTelegramId, v -> v, (k1, k2) -> k2));
            Map<Long, Integer> newIvMap = wodiUserLogDao.findByTgIdToday(inviteeIds).stream()
                    .collect(Collectors.groupingBy(WodiUserLog::getTelegramId,
                            Collectors.summingInt(bean ->
                                    (int) (bean.getFraction() * 0.3 + bean.getTiv() * 0.1))
                    ));
            MutableTriple<Integer, Integer, Integer> ivTriple = null;
            if (null == embyUser.getCh() || !DateUtil.isSameDay(embyUser.getCh(), DateUtil.date())) {
                ivTriple = new MutableTriple<>(0, 0, 0);
                // 随机基础base
                ivTriple.setLeft(RandomUtil.randomInt(1, 10));
                // 游戏加成
                ivTriple.setMiddle(WdUtil.scoreToLv(user.getFraction()));
                // 发放弟子奖励
                int ivTotal = CollUtil.size(yesInviteeIds);
                if (MapUtil.isNotEmpty(ivMap)) {
                    xInviteDao.updateCollectTime(yesInviteeIds, DateUtil.yesterday());
                    ivTotal = Math.max(ivMap.values().stream().mapToInt(v -> v).sum(), ivTotal);
                }
                ivTriple.setRight(ivTotal);
                int ivUp = ivTriple.left + ivTriple.middle + ivTriple.right;
                if (isAdmin) {
                    ivUp += 3;
                }
                embyDao.upIv(userId, ivUp);
                embyDao.checkIn(userId);
                embyUser.setIv(embyUser.getIv() + ivUp);
            }

            SendPhoto sendPhoto = SendPhoto.builder()
                    .chatId(chatId).caption(WdUtil.getCheckRecord(user, embyUser, wodiTops, topMap,
                            xInvites, embyMap, newIvMap, ivTriple, chatMember))
                    .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                            "static/pic/s{}/lv{}.webp", CURRENT_SEASON, WdUtil.scoreToLv(user.getFraction()))),
                            "谁是卧底个人信息"))
                    .build();
            tgService.sendPhoto(sendPhoto, 300 * 1000);
        } finally {
            CACHE_CHECKIN.put(userId, 1);
        }
    }

    /**
     * 处理 个人记录
     *
     * @param chatId 聊天id
     * @param userId 用户id
     */
    public void handleRecordCommand(String chatId, Long userId) {
        WodiUser user = wodiUserDao.findByTgId(userId);
        Emby embyUser = embyDao.findByTgId(userId);
        if (user == null || embyUser == null) {
            tgService.sendMsg(chatId, "您还未参与过游戏或者未在bot处登记哦~", 5 * 1000);
            return;
        }
        Integer costIv = 5;
        if (null == embyUser.getIv() || embyUser.getIv() < costIv) {
            tgService.sendMsg(chatId, "您的Dmail不足，无法查看个人信息", 5 * 1000);
            return;
        }
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            embyDao.upIv(userId, -costIv);
        }
        List<WodiTop> wodiTops = wodiTopDao.selectByTgId(userId);
        Map<Long, Integer> topMap = powerRankService.findTopByCache();
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId).caption(WdUtil.getRecord(user, embyUser, wodiTops, topMap))
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/s{}/lv{}.webp", CURRENT_SEASON, WdUtil.scoreToLv(user.getFraction()))),
                        "谁是卧底个人信息"))
                .build();
        tgService.sendPhoto(sendPhoto, 75 * 1000);
    }

    /**
     * 是否玩家用户
     *
     * @param chatId 聊天id
     * @param userId 用户id
     * @return boolean
     */
    public Emby isEmbyUser(String chatId, Long userId) {
        Emby embyUser = embyDao.findByTgId(userId);
        if (embyUser == null) {
            tgService.sendMsg(chatId, "您还未在bot处登记哦~", 5 * 1000);
        }
        return embyUser;
    }

    @SneakyThrows
    private void handleRankCommand(String chatId, Long userId) {
        Emby emby = isEmbyUser(chatId, userId);
        if (null == emby) {
            return;
        }
        Integer costIv = 10;
        if (null == emby.getIv() || emby.getIv() < costIv) {
            tgService.sendMsg(chatId, "您的Dmail不足，无法查看榜单", 5 * 1000);
            return;
        }
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            embyDao.upIv(userId, -costIv);
        }
        tgService.sendMsg(chatId, StrUtil.format(TIP_IN_RANK, costIv), 2 * 1000);

        List<WodiUser> rankUserList = wodiUserDao.selectRank();
        if (CollUtil.isEmpty(rankUserList)) {
            return;
        }
        RANK_CACHE.put("RANK", rankUserList);
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId).caption(WdUtil.getRank(rankUserList, 1))
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/s{}/排行榜.webp", CURRENT_SEASON)), "名人榜"))
                .replyMarkup(TgUtil.rankPageBtn(1, CollUtil.size(rankUserList)))
                .build();
        GlobalUtil.rankMsg = tgService.sendPhoto(sendPhoto, 300 * 1000);
        // Console.log(rankMsg);
    }

    @SneakyThrows
    private void handleRealRankCommand(String chatId, Long userId) {
        Emby emby = isEmbyUser(chatId, userId);
        if (null == emby) {
            return;
        }
        List<Map.Entry<Long, Integer>> top20 = powerRankService.findTop20ListByCache();
        if (CollUtil.isEmpty(top20)) {
            return;
        }

        Integer costIv = 15;
        if (null == emby.getIv() || emby.getIv() < costIv) {
            tgService.sendMsg(chatId, "您的Dmail不足，无法查看榜单", 5 * 1000);
            return;
        }
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            embyDao.upIv(userId, -costIv);
        }
        tgService.sendMsg(chatId, StrUtil.format(TIP_IN_RANK, costIv), 2 * 1000);

        // 通过id获取用户信息
        List<WodiUser> userList = wodiUserDao.findByTgId(top20.stream().map(Map.Entry::getKey).toList());
        Map<Long, WodiUser> userMap = userList.stream().collect(
                Collectors.toMap(WodiUser::getTelegramId, v -> v, (k1, k2) -> k2));

        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId).caption(WdUtil.getRealRank(top20, userMap))
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/s{}/名人榜.webp", CURRENT_SEASON)), "名人榜"))
                .build();
        GlobalUtil.rankMsg = tgService.sendPhoto(sendPhoto, 300 * 1000);
    }

    @SneakyThrows
    private void handleTopCommand(String chatId, Long userId, String text) {
        Emby emby = isEmbyUser(chatId, userId);
        if (null == emby) {
            return;
        }
        Integer costIv = 3;
        if (null == emby.getIv() || emby.getIv() < costIv) {
            tgService.sendMsg(chatId, "您的Dmail不足，无法查看榜单", 5 * 1000);
            return;
        }
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            embyDao.upIv(userId, -costIv);
        }
        tgService.sendMsg(chatId, StrUtil.format(TIP_IN_RANK, costIv), 2 * 1000);

        String seasonStr = StrUtil.trim(StrUtil.removePrefix(text, 大佬榜单));
        if (!NumberUtil.isNumber(seasonStr)) {
            seasonStr = EMPTY;
        }
        Integer season = StrUtil.isBlank(seasonStr) ? CURRENT_SEASON : Integer.valueOf(seasonStr);
        List<WodiTop> topList = wodiTopDao.selectTop(season);
        if (CollUtil.isEmpty(topList)) {
            return;
        }
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId).caption(WdUtil.getTop(topList, season))
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/s{}/Top飞升.webp", season)), "Top飞升"))
                .build();
        tgService.sendPhoto(sendPhoto, 300 * 1000);
    }

    /**
     * 按照排行榜翻页
     *
     * @param pageNum 书籍页码
     */
    public void handleEditRank(Integer pageNum) {
        if (GlobalUtil.rankMsg == null) {
            return;
        }
        List<WodiUser> rankUserList = RANK_CACHE.get("RANK");
        if (CollUtil.isEmpty(rankUserList)) {
            rankUserList = wodiUserDao.selectRank();
        }
        tgService.editMsg(GlobalUtil.rankMsg, WdUtil.getRank(rankUserList, pageNum),
                TgUtil.rankPageBtn(pageNum, CollUtil.size(rankUserList)));
    }

    /**
     * 处理 开启新游戏指令
     *
     * @param user 用户
     * @param chat 聊天
     */
    private void handleNewGameCommand(User user, Chat chat) {
        String chatId = chat.getId().toString();
        String userId = user.getId().toString();
        if (!StrUtil.equals(chatId, tgService.getGroup())) {
            tgService.sendMsg(chatId, NO_AUTH_GROUP);
            log.error("非授权群组私自拉bot入群已被发现：{}, chat: {}", chatId, chat);
            return;
        }
        if (!WdUtil.isInGameTime()) {
            tgService.sendMsg(chatId, CURFEW_GAME_TIME);
            return;
        }
        Game game = GameListUtil.getGame(chatId);
        if (game == null) {
            // 发言结束或者管理可以直接开
            if (!CollUtil.contains(tgService.getAdmins(), userId) && GAME_SPEAK_CNT.get() > 0) {
                tgService.sendMsg(chatId,
                        StrUtil.format(SPEAK_TIME_LIMIT, GAME_SPEAK_CNT.get()), 15 * 1000);
                return;
            }

            // 不存在则创建新游戏
            tgService.sendMsg(chatId,
                    StrUtil.format(userCreateGame, TgUtil.tgNameOnUrl(user)), 5 * 1000);
            GameListUtil.createGame(chat, user);
        } else {
            // 存在则加入 游戏已经开始就提示
            if (game.getStatus() == GameStatus.等待加入) {
                game.joinGame(user);
                return;
            }
            tgService.sendMsg(chatId,
                    StrUtil.format(IN_GAMING, TgUtil.tgNameOnUrl(user)), 5 * 1000);
        }
    }

    /**
     * 处理退出命令
     *
     * @param message 消息
     * @param chatId  聊天id
     * @param userId  用户id
     */
    private void handleExitCommand(Message message, String chatId, Long userId) {
        Game game = GameListUtil.getGame(chatId);
        if (game == null || game.getMember(userId) == null) {
            return;
        }
        if (game.getStatus() == GameStatus.游戏结算中) {
            tgService.sendMsg(new SendMessage(chatId, GAME_SETTLEMENT), GameStatus.游戏结算中, game);
            return;
        }
        exitGame(message, chatId, userId);
    }

    /**
     * 关闭游戏
     *
     * @param message 消息
     * @param chatId  聊天id
     * @param userId  用户id
     */
    private void exitGame(Message message, String chatId, Long userId) {
        Game game = GameListUtil.getGame(chatId);
        if (!game.homeOwner.getId().equals(userId)) {
            tgService.sendMsg(chatId, EXIT_GAME_ERROR, 5 * 1000);
            return;
        }
        game.setStatus(GameStatus.游戏关闭);
        if (game.rotate < 2) {
            embyDao.upIv(userId, 9);
            tgService.sendMsg(chatId, StrUtil.format(EXIT_GAME2, message.getFrom().getFirstName()));
        } else {
            embyDao.upIv(userId, -3);
            tgService.sendMsg(chatId, StrUtil.format(EXIT_GAME, message.getFrom().getFirstName()));
        }
    }

    /**
     * 游戏讨论
     *
     * @param message 消息
     */
    public void speak(Message message) {
        String text = message.getText();
        if (!StrUtil.startWith(text, "，")) {
            return;
        }
        Game game = GameListUtil.getGame(message.getChatId().toString());
        if (game == null || !讨论时间.equals(game.getStatus())) {
            return;
        }
        text = StrUtil.removePrefix(text, "，");
        text = ChineseUtil.toSimple(text);
        // 第一轮不能爆；非。开头不能爆；第二轮时如果第一轮发言人数<2不能爆
        boolean canBoom = game.rotate != 1 && StrUtil.startWith(text, "。")
                && !(game.rotate == 2 && game.firstSpeakList.size() < 2);
        if (canBoom) {
            text = StrUtil.removePrefix(text, "。");
            game.boom(message, text);
        } else {
            game.speak(message, text);
        }
    }

    /**
     * 游戏讨论
     *
     * @param message 消息
     */
    public void needDel(Message message) {
        if (null == message || !message.hasText()) {
            return;
        }
        String text = message.getText();
        if (!StrUtil.containsAny(text, "#WodiInfo ", "#WodiRank ",
                "#WodiTop ", "#WodiRealRank ", "世界线", "Dmail风云录")) {
            return;
        }
        MsgDelUtil.addAutoDelMsg(message, 10 * 1000);
    }
}