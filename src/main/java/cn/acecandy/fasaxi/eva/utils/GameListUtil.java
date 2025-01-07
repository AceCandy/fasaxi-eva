package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.dao.entity.WodiGroup;
import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.hutool.core.collection.ConcurrentHashSet;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Optional;
import java.util.Set;

/**
 * 游戏列表 工具类
 *
 * @author AceCandy
 * @since 2025/01/07
 */
public final class GameListUtil {

    private GameListUtil() {
    }

    private static final Set<Game> GAME_LIST = new ConcurrentHashSet<>();

    public static void createGame(WodiGroup group, Message message, User user) {
        GAME_LIST.add(new Game(group, message, user));
    }

    public static void removeGame(Game game) {
        if (game != null) {
            game.run = false;
            GAME_LIST.remove(game);
        }
    }

    /**
     * 获取游戏
     *
     * @param chatId 聊天id
     * @return {@link Game }
     */
    public static Game getGame(Long chatId) {
        Optional<Game> game = GAME_LIST.stream()
                .filter(g -> g != null && chatId.equals(g.chatId))
                .findFirst();
        return game.orElse(null);
    }
}