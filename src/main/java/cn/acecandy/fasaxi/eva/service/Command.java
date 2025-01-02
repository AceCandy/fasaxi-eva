package cn.acecandy.fasaxi.eva.service;

import cn.acecandy.fasaxi.eva.bin.GameStatus;
import cn.acecandy.fasaxi.eva.bin.GameUtil;
import cn.acecandy.fasaxi.eva.bin.TgUtil;
import cn.acecandy.fasaxi.eva.bot.impl.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.game.Game;
import cn.acecandy.fasaxi.eva.game.GameList;
import cn.acecandy.fasaxi.eva.sql.entity.WodiGroup;
import cn.acecandy.fasaxi.eva.sql.entity.WodiTop;
import cn.acecandy.fasaxi.eva.sql.entity.WodiUser;
import cn.acecandy.fasaxi.eva.sql.service.EmbyDao;
import cn.acecandy.fasaxi.eva.sql.service.WodiGroupDao;
import cn.acecandy.fasaxi.eva.sql.service.WodiTopDao;
import cn.acecandy.fasaxi.eva.sql.service.WodiUserDao;
import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.acecandy.fasaxi.eva.bin.Constants.CURRENT_SEASON;
import static cn.acecandy.fasaxi.eva.bin.Constants.GAME_SETTLEMENT;
import static cn.acecandy.fasaxi.eva.bin.Constants.InTheGame;
import static cn.acecandy.fasaxi.eva.bin.Constants.SPEAK_TIME_LIMIT_CNT;
import static cn.acecandy.fasaxi.eva.bin.Constants.TIP_HELP;
import static cn.acecandy.fasaxi.eva.bin.Constants.TIP_IN_GROUP;
import static cn.acecandy.fasaxi.eva.bin.Constants.TIP_IN_RANK;
import static cn.acecandy.fasaxi.eva.bin.Constants.exitGame;
import static cn.acecandy.fasaxi.eva.bin.Constants.userCreateGame;

/**
 * 命令处理类，转入命令 仅命令名 （无‘/’，无‘@****’）
 */
@Component
public class Command {

    @Resource
    private EmbyTelegramBot tgBot;

    @Resource
    private WodiGroupDao wodiGroupDao;
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

    private final static String NEW_GAME = "/wodi";
    private final static String HELP = "/wodi_help";
    private final static String EXIT = "/wodi_exit";
    private final static String RECORD = "/wodi_record";
    private final static String RANK = "/wodi_rank";
    private final static String TOP = "/wodi_top";

    @Resource
    private EmbyDao embyDao;

    public void process(@NotNull String command, Message message, boolean groupMessage) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!groupMessage && command.equals(NEW_GAME)) {
            tgBot.sendMessage(new SendMessage(chatId.toString(), TIP_IN_GROUP), 15 * 1000);
            return;
        }

        /*if (StrUtil.equalsAny(command, RANK, EXIT) && !CollUtil.contains(tgBot.getAdmins(), userId)) {
            tgBot.sendMessage(new SendMessage(chatId.toString(), TIP_IN_OWNER), 15 * 1000);
            return;
        }*/

        switch (command) {
            case RECORD:
                handleRecordCommand(message, userId);
                break;
            case RANK:
                handleRankCommand(chatId, message);
                break;
            case TOP:
                handleTopCommand(chatId, message);
                // handleRankCommand(chatId, message);
                break;
            case NEW_GAME:
                handleNewGameCommand(message, groupMessage, chatId);
                break;
            case HELP:
                handleHelpCommand(message, groupMessage);
                break;
            case EXIT:
                handleExitCommand(message, chatId, userId);
                break;
            default:
                break;
        }
    }

    /*private void handleStartCommand(Long chatId, boolean groupMessage, Long userId) {
        if (groupMessage) {
            return;
        }
        GameList.UnsentUserWord unsentUserWords = GameList.getUnsentUserWords(userId);
        if (unsentUserWords == null) {
            return;
        }
        tgBot.sendMessage(new SendMessage(chatId.toString(), unsentUserWords.messageText));
    }*/

    private void handleRecordCommand(Message message, Long userId) {
        WodiUser user = wodiUserDao.findByTgId(userId);
        if (user == null) {
            return;
        }
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(message.getChatId().toString())
                .photo(new InputFile(
                        ResourceUtil.getStream("static/pic/谁是卧底个人信息.jpeg"), "谁是卧底个人信息"))
                .caption(GameUtil.getRecord(user))
                .parseMode(ParseMode.HTML)
                .build();
        tgBot.sendPhoto(sendPhoto, 60 * 1000);
    }

    @SneakyThrows
    private void handleRankCommand(Long chatId, Message message) {
        if (!CollUtil.contains(tgBot.getAdmins(), message.getFrom().getId())) {
            embyDao.upIv(message.getFrom().getId(), -20);
        }
        tgBot.sendMessage(new SendMessage(chatId.toString(),
                StrUtil.format(TIP_IN_RANK, TgUtil.tgNameOnUrl(message.getFrom()))), 1 * 1000);

        List<WodiUser> rankUserList = wodiUserDao.selectRank();
        if (CollUtil.isEmpty(rankUserList)) {
            return;
        }
        rankUserListMap.put("RANK", rankUserList);
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(message.getChatId().toString())
                .photo(new InputFile(
                        ResourceUtil.getStream("static/pic/谁是卧底名人榜.png"), "谁是卧底名人榜"))
                .caption(GameUtil.getRank(rankUserList, 1))
                .replyMarkup(TgUtil.rankPageBtn(1, CollUtil.size(rankUserList)))
                .parseMode(ParseMode.HTML)
                .build();
        rankMsg = tgBot.sendPhoto(sendPhoto, 300 * 1000);
    }

    @SneakyThrows
    private void handleTopCommand(Long chatId, Message message) {
        if (!CollUtil.contains(tgBot.getAdmins(), message.getFrom().getId())) {
            embyDao.upIv(message.getFrom().getId(), -20);
        }
        tgBot.sendMessage(new SendMessage(chatId.toString(),
                StrUtil.format(TIP_IN_RANK, TgUtil.tgNameOnUrl(message.getFrom()))), 100);
        String seasonStr = StrUtil.trim(StrUtil.removePrefix(message.getText(), "/wodi_top"));
        Integer season = StrUtil.isBlank(seasonStr) ? CURRENT_SEASON : Integer.valueOf(seasonStr);
        List<WodiTop> topList = wodiTopDao.selectTop(season);
        if (CollUtil.isEmpty(topList)) {
            return;
        }
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(message.getChatId().toString())
                .photo(new InputFile(ResourceUtil.getStream(StrUtil.format(
                        "static/pic/Top飞升-{}-压缩.jpg", GameUtil.getTopBySeason(season))),
                        GameUtil.getTopBySeason(season)))
                .caption(GameUtil.getTop(topList, season))
                .parseMode(ParseMode.HTML)
                .build();
        tgBot.sendPhoto(sendPhoto, 300 * 1000);
    }

    public void handleEditRank(Integer pageNum) {
        if (rankMsg == null) {
            return;
        }
        List<WodiUser> rankUserList = CollUtil.newArrayList();
        if (rankUserListMap.containsKey("RANK")) {
            rankUserList = rankUserListMap.get("RANK");
        } else {
            rankUserList = wodiUserDao.selectRank();
        }
        tgBot.editMessage(rankMsg, GameUtil.getRank(rankUserList, pageNum),
                TgUtil.rankPageBtn(pageNum, CollUtil.size(rankUserList)));
    }

    private void handleNewGameCommand(Message message, boolean groupMessage, Long chatId) {
        if (!groupMessage) {
            return;
        }
        // 发言结束或者管理可以直接开
        if (!CollUtil.contains(tgBot.getAdmins(), message.getFrom().getId()) && SPEAK_TIME_CNT.get() > 0) {
            SendMessage sendMessage = new SendMessage(chatId.toString(),
                    StrUtil.format(SPEAK_TIME_LIMIT_CNT, SPEAK_TIME_CNT.get()));
            tgBot.sendMessage(sendMessage, 15 * 1000);
            return;
        }

        Game game = GameList.getGame(chatId);
        // TelegramGroup group = TelegramGroupService.getGroup(chatId.toString());
        WodiGroup group = wodiGroupDao.findByGroupIdIfExist(chatId);
        if (game == null) {
            createNewGame(message, chatId, group);
        } else {
            handleExistingGame(message, chatId, game, group);
        }
    }

    private void createNewGame(Message message, Long chatId, WodiGroup group) {
        User user = message.getFrom();
        SendMessage sendMessage = new SendMessage(chatId.toString(),
                StrUtil.format(userCreateGame, TgUtil.tgNameOnUrl(user)));
        tgBot.sendMessage(sendMessage);
        GameList.createGame(group, message, user);
    }

    private void handleExistingGame(Message message, Long chatId, Game game, WodiGroup group) {
        if (game.getStatus() != GameStatus.游戏结算中) {
            SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(StrUtil.format(InTheGame, TgUtil.tgNameOnUrl(message.getFrom())))
                    .parseMode(ParseMode.HTML);

            if (game.getStatus() == GameStatus.等待加入) {
                sendMessageBuilder.replyMarkup(TgUtil.getJoinGameMarkup(false, game));
                tgBot.sendMessage(sendMessageBuilder.build(), 0, GameStatus.等待加入, game);
            } else {
                tgBot.sendMessage(sendMessageBuilder.build(), 10000);
            }
        } else {
            sendGameSettlementMessage(chatId, game);
        }
    }

    private void sendGameSettlementMessage(Long chatId, Game game) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(GAME_SETTLEMENT)
                .build();
        tgBot.sendMessage(sendMessage, 0, GameStatus.游戏结算中, game);
    }

    private void handleHelpCommand(Message message, boolean groupMessage) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text(TIP_HELP)
                .build();
        tgBot.sendMessage(sendMessage, 300 * 1000);
    }

    private void handleExitCommand(Message message, Long chatId, Long userId) {
        Game game = GameList.getGame(chatId);
        if (game != null && game.getMember(userId) != null) {
            // TelegramGroup group = TelegramGroupService.getGroup(chatId.toString());
            WodiGroup group = wodiGroupDao.findByGroupIdIfExist(chatId);
            if (game.getStatus() != GameStatus.游戏结算中) {
                exitGame(message, chatId.toString(), userId, group);
            } else {
                sendGameSettlementMessage(chatId, game);
            }
        }
    }

    private void exitGame(Message message, String chatIdStr, Long userId, WodiGroup group) {
        Game game = GameList.getGame(message.getChatId());
        game.setStatus(GameStatus.游戏关闭);
        // wodiUserDao.upExitGame(userId);
        // wodiUserDao.upFraction(userId, -2);
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatIdStr)
                .text(StrUtil.format(exitGame, TgUtil.tgNameOnUrl(message.getFrom())))
                .build();
        tgBot.sendMessage(sendMessage);
    }
}