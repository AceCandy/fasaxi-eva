package cn.acecandy.fasaxi.eva.bot.game;

import cn.acecandy.fasaxi.eva.bot.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.common.enums.GameStatus;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.entity.WodiTop;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiTopDao;
import cn.acecandy.fasaxi.eva.dao.service.WodiUserDao;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.acecandy.fasaxi.eva.utils.GameUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.*;
import static cn.hutool.core.text.CharSequenceUtil.EMPTY;

/**
 * 命令处理类，转入命令 仅命令名 （无‘/’，无‘@****’）
 */
@Component
public class Command {

    @Resource
    private EmbyTelegramBot tgBot;
    @Resource
    private WodiUserDao wodiUserDao;
    @Resource
    private WodiTopDao wodiTopDao;

    /**
     * 全局发言时间数量
     */
    public final static AtomicInteger SPEAK_TIME_CNT =
            new AtomicInteger(RandomUtil.randomInt(50, 150));

    public volatile Message rankMsg;
    public volatile TimedCache<String, List<WodiUser>> rankUserListMap
            = CacheUtil.newTimedCache(600 * 1000);

    private final static String NEW_GAME = "/wd";
    private final static String RECORD = "/wd_info";
    private final static String RANK = "/wd_rank";
    private final static String TOP = "/wd_top";
    private final static String EXIT = "/wd_exit";
    private final static String HELP = "/wd_help";

    @Resource
    private EmbyDao embyDao;

    public void process(@NotNull String command, Message message, boolean groupMessage) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!groupMessage && command.equals(NEW_GAME)) {
            tgBot.sendMessage(chatId, TIP_IN_GROUP, 10 * 1000);
            return;
        }

        switch (command) {
            case RECORD:
                handleRecordCommand(chatId, userId);
                break;
            case RANK:
                handleRankCommand(chatId, userId);
                break;
            case TOP:
                handleTopCommand(chatId, userId, message.getText());
                break;
            case NEW_GAME:
                handleNewGameCommand(message.getFrom(), message.getChat(), userId);
                break;
            case HELP:
                tgBot.sendMessage(chatId, TIP_HELP, 300 * 1000);
                break;
            case EXIT:
                handleExitCommand(message, chatId, userId);
                break;
            default:
                break;
        }
    }

    /**
     * 处理 个人记录
     *
     * @param chatId 聊天id
     * @param userId 用户id
     */
    private void handleRecordCommand(Long chatId, Long userId) {
        WodiUser user = wodiUserDao.findByTgId(userId);
        Emby embyUser = embyDao.findByTgId(userId);
        if (user == null || embyUser == null) {
            tgBot.sendMessage(chatId, "您还未参与过游戏或者未在助手处登记哦~", 10 * 1000);
            return;
        }

        if (!CollUtil.contains(tgBot.getAdmins(), userId)) {
            embyDao.upIv(userId, -2);
        }
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId.toString()).caption(GameUtil.getRecord(user, embyUser))
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/s{}/lv{}.webp", CURRENT_SEASON, GameUtil.level(user.getFraction()))),
                        "谁是卧底个人信息"))
                .build();
        tgBot.sendPhoto(sendPhoto, 75 * 1000);
    }

    /**
     * 是否玩家用户
     *
     * @param chatId 聊天id
     * @param userId 用户id
     * @return boolean
     */
    private boolean isEmbyUser(Long chatId, Long userId) {
        Emby embyUser = embyDao.findByTgId(userId);
        if (embyUser == null) {
            tgBot.sendMessage(chatId, "您还未在助手处登记哦~", 5 * 1000);
            return false;
        }
        return true;
    }

    @SneakyThrows
    private void handleRankCommand(Long chatId, Long userId) {
        if (!isEmbyUser(chatId, userId)) {
            return;
        }
        if (!CollUtil.contains(tgBot.getAdmins(), userId)) {
            embyDao.upIv(userId, -15);
        }
        tgBot.sendMessage(chatId, TIP_IN_RANK, 2 * 1000);

        List<WodiUser> rankUserList = wodiUserDao.selectRank();
        if (CollUtil.isEmpty(rankUserList)) {
            return;
        }
        rankUserListMap.put("RANK", rankUserList);
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId).caption(GameUtil.getRank(rankUserList, 1))
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/s{}/名人榜.webp", CURRENT_SEASON)), "名人榜"))
                .replyMarkup(TgUtil.rankPageBtn(1, CollUtil.size(rankUserList)))
                .build();
        rankMsg = tgBot.sendPhoto(sendPhoto, 300 * 1000);
    }

    @SneakyThrows
    private void handleTopCommand(Long chatId, Long userId, String text) {
        if (!isEmbyUser(chatId, userId)) {
            return;
        }
        if (!CollUtil.contains(tgBot.getAdmins(), userId)) {
            embyDao.upIv(userId, -10);
        }
        tgBot.sendMessage(chatId, TIP_IN_TOP, 2 * 1000);

        String seasonStr = StrUtil.trim(StrUtil.removePrefix(text, TOP));
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
        tgBot.sendPhoto(sendPhoto, 300 * 1000);
    }

    /**
     * 按照排行榜翻页
     *
     * @param pageNum 书籍页码
     */
    public void handleEditRank(Integer pageNum) {
        if (rankMsg == null) {
            return;
        }
        List<WodiUser> rankUserList = rankUserListMap.containsKey("RANK") ?
                rankUserListMap.get("RANK") : wodiUserDao.selectRank();
        tgBot.editMessage(rankMsg, GameUtil.getRank(rankUserList, pageNum),
                TgUtil.rankPageBtn(pageNum, CollUtil.size(rankUserList)));
    }

    /**
     * 处理 开启新游戏指令
     *
     * @param user   用户
     * @param chat   聊天
     * @param userId 用户id
     */
    private void handleNewGameCommand(User user, Chat chat, Long userId) {
        Game game = GameListUtil.getGame(chat.getId());
        if (game == null) {
            // 发言结束或者管理可以直接开
            if (!CollUtil.contains(tgBot.getAdmins(), userId) && SPEAK_TIME_CNT.get() > 0) {
                tgBot.sendMessage(chat.getId(),
                        StrUtil.format(SPEAK_TIME_LIMIT, SPEAK_TIME_CNT.get()), 15 * 1000);
                return;
            }

            // 不存在则创建新游戏
            tgBot.sendMessage(chat.getId(),
                    StrUtil.format(userCreateGame, TgUtil.tgNameOnUrl(user)), 5 * 1000);
            GameListUtil.createGame(chat, user);
        } else {
            // 存在则加入 游戏已经开始就提示
            if (game.getStatus() == GameStatus.等待加入) {
                game.joinGame(user);
                return;
            }
            tgBot.sendMessage(chat.getId(),
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
    private void handleExitCommand(Message message, Long chatId, Long userId) {
        Game game = GameListUtil.getGame(chatId);
        if (game == null || game.getMember(userId) == null) {
            return;
        }
        if (game.getStatus() == GameStatus.游戏结算中) {
            tgBot.sendMessage(new SendMessage(chatId.toString(), GAME_SETTLEMENT), GameStatus.游戏结算中, game);
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
    private void exitGame(Message message, Long chatId, Long userId) {
        Game game = GameListUtil.getGame(chatId);
        if (!game.homeOwner.getId().equals(userId)) {
            tgBot.sendMessage(chatId, EXIT_GAME_ERROR, 5 * 1000);
            return;
        }
        wodiUserDao.upFraction(userId, -3);
        game.setStatus(GameStatus.游戏关闭);
        tgBot.sendMessage(chatId, StrUtil.format(EXIT_GAME, message.getFrom().getFirstName()), 15 * 1000);
    }
}