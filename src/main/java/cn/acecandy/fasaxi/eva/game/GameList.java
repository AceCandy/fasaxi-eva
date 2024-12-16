package cn.acecandy.fasaxi.eva.game;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.acecandy.fasaxi.eva.sql.entity.WodiGroup;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Optional;
import java.util.Set;

@Component
public class GameList {

    private static final Set<Game> GAME_LIST = new ConcurrentHashSet<>();
    // public static final Set<UnsentUserWord> UNSENT_USER_WORDS = new ConcurrentHashSet<>();

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

    /*public static UnsentUserWord getUnsentUserWords(long userId) {
        synchronized (UNSENT_USER_WORDS) {
            Iterator<UnsentUserWord> iterator = UNSENT_USER_WORDS.iterator();
            while (iterator.hasNext()) {
                UnsentUserWord userWord = iterator.next();
                if (userWord != null && userWord.userId == userId) {
                    iterator.remove(); // 使用迭代器安全地删除元素
                    return userWord;
                }
            }
        }
        return null;
    }*/

    /*public static class UnsentUserWord {
        public String messageText;
        public long createTime;
        long userId;
        public Game game;

        public UnsentUserWord(String messageText, long createTime, long userId, Game game) {
            this.messageText = messageText;
            this.createTime = createTime;
            this.userId = userId;
            this.game = game;
        }

    }*/
}