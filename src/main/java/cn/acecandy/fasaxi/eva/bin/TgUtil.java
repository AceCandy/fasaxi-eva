package cn.acecandy.fasaxi.eva.bin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import cn.acecandy.fasaxi.eva.game.Game;
import cn.acecandy.fasaxi.eva.game.GameList;
import cn.acecandy.fasaxi.eva.service.ButtonEvent;
import cn.acecandy.fasaxi.eva.sql.entity.WodiTop;
import cn.acecandy.fasaxi.eva.sql.entity.WodiUser;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static cn.acecandy.fasaxi.eva.bin.Constants.EXIT;
import static cn.acecandy.fasaxi.eva.bin.Constants.JOIN_GAME;
import static cn.acecandy.fasaxi.eva.bin.Constants.READY;
import static cn.acecandy.fasaxi.eva.bin.Constants.START;
import static cn.acecandy.fasaxi.eva.bin.Constants.ViewWord;
import static cn.acecandy.fasaxi.eva.bin.Constants.abstain;

/**
 * tg工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class TgUtil {
    private TgUtil() {
    }

    /**
     * tg名称
     *
     * @param user 用户
     * @return {@link String }
     */
    public static String tgNameOnUrl(User user) {
        return StrUtil.format("<a href=\"tg://user?id={}\">{}</a>", user.getId(), tgName(user));
    }

    public static String tgNameOnUrl(WodiUser user) {
        return StrUtil.format("<a href='tg://user?id={}'>{}</a>", user.getTelegramId(), tgName(user));
    }

    public static String tgNameOnUrl(WodiTop user) {
        return StrUtil.format("<a href='tg://user?id={}'>{}</a>", user.getTelegramId(), tgName(user));
    }

    public static String tgNameOnUrl(Game.Member user) {
        return StrUtil.format("<a href=\"tg://user?id={}\">{}</a>", user.id, tgName(user.user));
    }

    /**
     * tg名称
     *
     * @param user 用户
     * @return {@link String }
     */
    public static String tgName(User user) {
        String name = user.getFirstName();
        if (StrUtil.isNotBlank(user.getLastName())) {
            name += " " + user.getLastName();
        }
        return StrUtil.subPre(name, 20);
    }

    public static String tgName(WodiUser user) {
        String name = user.getFirstName();
        if (StrUtil.isNotBlank(user.getLastName())) {
            name += " " + user.getLastName();
        }
        return StrUtil.subPre(name, 20);
    }

    public static String tgName(WodiTop user) {
        String name = user.getFirstName();
        if (StrUtil.isNotBlank(user.getLastName())) {
            name += " " + user.getLastName();
        }
        return StrUtil.subPre(name, 20);
    }

    /**
     * 游戏讨论
     *
     * @param message      消息
     */
    public static void gameSpeak(Message message) {
        String text = message.getText();
        if (StrUtil.isNotBlank(text) && StrUtil.startWith(text, "，")) {
            Game game = GameList.getGame(message.getChatId());
            if (game != null) {
                game.speak(message.getFrom().getId());
            }
        }
    }

    /**
     * 获取加入游戏标记
     *
     * @param startButton 启动按钮
     * @return {@link InlineKeyboardMarkup }
     */
    public static InlineKeyboardMarkup getJoinGameMarkup(boolean startButton, Game game) {
        InlineKeyboardButton joinGame = new InlineKeyboardButton(JOIN_GAME);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("action", ButtonEvent.ACTION_JOIN_GAME);
        joinGame.setCallbackData(jsonObject.toString());
        List<InlineKeyboardRow> rows = CollUtil.newArrayList();
        rows.add(new InlineKeyboardRow(joinGame));
        if (startButton) {
            InlineKeyboardButton readyGame = new InlineKeyboardButton(READY);
            JSONObject readyJn = new JSONObject();
            readyJn.put("action", ButtonEvent.ACTION_READY);
            readyGame.setCallbackData(readyJn.toString());

            InlineKeyboardButton exitGame = new InlineKeyboardButton(EXIT);
            JSONObject exitJn = new JSONObject();
            exitJn.put("action", ButtonEvent.ACTION_EXIT);
            exitGame.setCallbackData(exitJn.toString());

            InlineKeyboardRow rowInline2 = new InlineKeyboardRow(readyGame, exitGame);
            if (GameUtil.isAllMemberReady(game)) {
                InlineKeyboardButton openGame = new InlineKeyboardButton(START);
                JSONObject openJn = new JSONObject();
                openJn.put("action", ButtonEvent.ACTION_OPEN);
                openGame.setCallbackData(openJn.toString());
                rowInline2.add(openGame);
            }
            rows.add(rowInline2);
        }
        return new InlineKeyboardMarkup(rows);
    }

    public static InlineKeyboardMarkup getVoteMarkup(Game game) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Game.Member member : game.memberList) {
            if (member.survive) {
                InlineKeyboardRow rowInline = new InlineKeyboardRow();
                InlineKeyboardButton button = new InlineKeyboardButton(TgUtil.tgName(member.user));
                JSONObject data = new JSONObject();
                data.put("action", ButtonEvent.ACTION_VOTE);
                data.put("to", member.id);
                button.setCallbackData(data.toString());
                rowInline.add(button);
                rows.add(rowInline);
            }
        }
        InlineKeyboardRow rowInline = new InlineKeyboardRow();
        InlineKeyboardButton button = new InlineKeyboardButton(abstain);
        JSONObject data = new JSONObject();
        data.put("action", ButtonEvent.ACTION_VOTE);
        data.put("to", -1);
        button.setCallbackData(data.toString());
        rowInline.add(button);
        rows.add(rowInline);
        return new InlineKeyboardMarkup(rows);
    }

    /**
     * 获取查看词
     *
     * @param botUserName bot用户名
     * @return {@link InlineKeyboardMarkup }
     */
    public static InlineKeyboardMarkup getViewWord(String botUserName) {
        InlineKeyboardButton viewWord = new InlineKeyboardButton(ViewWord);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("action", ButtonEvent.ACTION_VIEW_WORD);
        viewWord.setCallbackData(jsonObject.toString());
        viewWord.setUrl("t.me/" + botUserName);
        List<InlineKeyboardRow> rows = CollUtil.newArrayList();
        rows.add(new InlineKeyboardRow(viewWord));
        return new InlineKeyboardMarkup(rows);
    }

    /**
     * 创建翻页按钮
     *
     * @return {@link InlineKeyboardMarkup }
     */
    public static InlineKeyboardMarkup rankPageBtn(int currentPage, int totalItems) {
        List<InlineKeyboardRow> rowList = CollUtil.newArrayList();

        // 计算总页数
        int totalPages = (int) Math.ceil((double) totalItems / 10);

        // 添加翻页按钮
        InlineKeyboardRow pageBtn = new InlineKeyboardRow();
        if (currentPage > 1) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton("⬅️ 上一页");
            JSONObject preJn = new JSONObject();
            preJn.put("action", ButtonEvent.PUBLIC_ACTION_RANKS + ":" + (currentPage - 1));
            prevButton.setCallbackData(preJn.toString());
            pageBtn.add(prevButton);
        }

        if (currentPage < totalPages) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton("下一页 ➡️");
            JSONObject nextJn = new JSONObject();
            nextJn.put("action", ButtonEvent.PUBLIC_ACTION_RANKS + ":" + (currentPage + 1));
            nextButton.setCallbackData(nextJn.toString());
            pageBtn.add(nextButton);
        }
        rowList.add(pageBtn);

        // 添加关闭按钮
        InlineKeyboardButton closeButton = new InlineKeyboardButton("❌ - 关闭");
        closeButton.setCallbackData("closeit");
        rowList.add(new InlineKeyboardRow(closeButton));

        // 设置键盘
        return new InlineKeyboardMarkup(rowList);
    }

    /**
     * 提取消息文本为command
     *
     * @param msgData     msg数据
     * @param botUserName bot用户名
     * @return {@link String }
     */
    public static String extractCommand(String msgData, String botUserName) {
        return CollUtil.getFirst(
                StrUtil.splitTrim(
                        StrUtil.removeAll(msgData, StrUtil.AT + botUserName),
                        StrUtil.SPACE
                )
        ).toLowerCase();
    }
}