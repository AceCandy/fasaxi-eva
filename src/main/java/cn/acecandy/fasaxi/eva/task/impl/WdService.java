package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.entity.WodiTop;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiTopDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserDao;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.acecandy.fasaxi.eva.utils.GameUtil;
import cn.acecandy.fasaxi.eva.utils.GlobalUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

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

    private final static String 新建游戏 = "/wd";
    private final static String 个人信息 = "/wd_info";
    private final static String 游戏排行 = "/wd_rank";
    private final static String 大佬榜单 = "/wd_top";
    private final static String 关闭游戏 = "/wd_exit";
    private final static String 游戏帮助 = "/wd_help";

    @Resource
    private EmbyDao embyDao;

    public void process(String cmd, Message message) {
        String chatId = message.getChatId().toString();
        if (!TgUtil.isGroupMsg(message)) {
            tgService.sendMsg(chatId, TIP_IN_GROUP, 10 * 1000);
            return;
        }
        Long userId = message.getFrom().getId();
        Integer msgId = message.getMessageId();
        switch (cmd) {
            case 新建游戏 -> handleNewGameCommand(message.getFrom(), message.getChat());
            case 个人信息 -> handleRecordCommand(chatId, userId);
            case 游戏排行 -> handleRankCommand(chatId, userId);
            case 大佬榜单 -> handleTopCommand(chatId, userId, message.getText());
            case 关闭游戏 -> handleExitCommand(message, chatId, userId);
            case 游戏帮助 -> tgService.sendMsg(chatId, TIP_HELP, 300 * 1000);
            default -> {
            }
        }
    }

    /**
     * 处理 个人记录
     *
     * @param chatId 聊天id
     * @param userId 用户id
     */
    private void handleRecordCommand(String chatId, Long userId) {
        WodiUser user = wodiUserDao.findByTgId(userId);
        Emby embyUser = embyDao.findByTgId(userId);
        if (user == null || embyUser == null) {
            tgService.sendMsg(chatId, "您还未参与过游戏或者未在助手处登记哦~", 5 * 1000);
            return;
        }
        Integer costIv = 2;
        if (embyUser.getIv() < costIv) {
            tgService.sendMsg(chatId, "您的Dmail不足，无法查看个人信息", 5 * 1000);
            return;
        }
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            embyDao.upIv(userId, -costIv);
        }
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId).caption(GameUtil.getRecord(user, embyUser))
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/s{}/lv{}.webp", CURRENT_SEASON, GameUtil.scoreToLv(user.getFraction()))),
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
            tgService.sendMsg(chatId, "您还未在助手处登记哦~", 5 * 1000);
        }
        return embyUser;
    }

    @SneakyThrows
    private void handleRankCommand(String chatId, Long userId) {
        Emby emby = isEmbyUser(chatId, userId);
        if (null == emby) {
            return;
        }
        Integer costIv = 15;
        if (emby.getIv() < costIv) {
            tgService.sendMsg(chatId, "您的Dmail不足，无法查看榜单", 5 * 1000);
            return;
        }
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            embyDao.upIv(userId, -costIv);
        }
        tgService.sendMsg(chatId, TIP_IN_RANK, 2 * 1000);

        List<WodiUser> rankUserList = wodiUserDao.selectRank();
        if (CollUtil.isEmpty(rankUserList)) {
            return;
        }
        RANK_CACHE.put("RANK", rankUserList);
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId).caption(GameUtil.getRank(rankUserList, 1))
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/s{}/名人榜.webp", CURRENT_SEASON)), "名人榜"))
                .replyMarkup(TgUtil.rankPageBtn(1, CollUtil.size(rankUserList)))
                .build();
        GlobalUtil.rankMsg = tgService.sendPhoto(sendPhoto, 300 * 1000);
        // Console.log(rankMsg);
    }

    @SneakyThrows
    private void handleTopCommand(String chatId, Long userId, String text) {
        Emby emby = isEmbyUser(chatId, userId);
        if (null == emby) {
            return;
        }
        Integer costIv = 10;
        if (emby.getIv() < costIv) {
            tgService.sendMsg(chatId, "您的Dmail不足，无法查看榜单", 5 * 1000);
            return;
        }
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            embyDao.upIv(userId, -costIv);
        }
        tgService.sendMsg(chatId, TIP_IN_TOP, 2 * 1000);

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
                .chatId(chatId).caption(GameUtil.getTop(topList, season))
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
        tgService.editMsg(GlobalUtil.rankMsg, GameUtil.getRank(rankUserList, pageNum),
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
        if (!GameUtil.isInGameTime()) {
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
        wodiUserDao.upFraction(userId, -3);
        game.setStatus(GameStatus.游戏关闭);
        tgService.sendMsg(chatId, StrUtil.format(EXIT_GAME, message.getFrom().getFirstName()));
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
}