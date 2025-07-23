package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.bot.game.GameUser;
import cn.acecandy.fasaxi.eva.dao.entity.WodiTop;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.EXIT;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.JOIN_GAME;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.READY;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.START;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.ViewWord;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.abstain;

/**
 * tg工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
@Slf4j
public final class TgUtil {
    private TgUtil() {
    }

    public final static AtomicInteger SB_BOX_CNT = new AtomicInteger(100);
    public final static ConcurrentLinkedDeque<String> SB_BOX_REGIST_NO =
            new ConcurrentLinkedDeque<>(CollUtil.newArrayList("KH6IP", "FCrH1"));
    public final static List<String> SB_BOX_GIFT = CollUtil.newCopyOnWriteArrayList(
            CollUtil.newArrayList(
                    "快活的空气", "快活的空气", "快活的空气", "快活的空气", "快活的空气", "快活的空气",
                    "司墨的微笑", "司墨的微笑", "司墨的微笑", "倒影的凝视", "一半的码子", "一半的码子", "爱的续期",
                    "四倍的幸运", "四倍的幸运", "三倍的祝福", "三倍的祝福", "三倍的祝福", "三倍的祝福", "三倍的祝福",
                    "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠",
                    "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠",
                    "双倍的回赠", "双倍的回赠", "双倍的回赠", "双倍的回赠", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命",
                    "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命",
                    "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命",
                    "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命",
                    "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命",
                    "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命",
                    "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命", "等价交换的宿命",
                    "等价交换的宿命", "真心的一半", "真心的一半", "真心的一半", "真心的一半", "真心的一半", "真心的一半",
                    "真心的一半", "真心的一半", "真心的一半", "真心的一半", "真心的一半", "真心的一半", "真心的一半", "真心的一半",
                    "真心的一半", "真心的一半", "真心的一半", "真心的一半", "真心的一半", "真心的一半"
            ));

    /**
     * tg名称
     *
     * @param user 用户
     * @return {@link String }
     */
    public static String tgNameOnUrl(User user) {
        if (null == user) {
            return "";
        }
        return StrUtil.format("<a href=\"tg://user?id={}\">{}</a>", user.getId(), tgName(user));
    }

    public static String tgNameOnUrl(WodiUser user) {
        if (null == user) {
            return "";
        }
        return StrUtil.format("<a href='tg://user?id={}'>{}</a>", user.getTelegramId(), tgName(user));
    }

    public static String tgNameOnUrl(WodiTop user) {
        if (null == user) {
            return "";
        }
        return StrUtil.format("<a href='tg://user?id={}'>{}</a>", user.getTelegramId(), tgName(user));
    }

    public static String tgNameOnUrl(GameUser user) {
        return tgNameOnUrl(user.user);
    }

    /**
     * tg名称
     *
     * @param user 用户
     * @return {@link String }
     */
    public static String tgName(User user) {
        String name = user.getFirstName();
        /*if (StrUtil.isNotBlank(user.getLastName())) {
            name += " " + user.getLastName();
        }*/
        return StrUtil.subPre(name, 20);
    }

    public static String tgName(WodiUser user) {
        String name = user.getFirstName();
        /*if (StrUtil.isNotBlank(user.getLastName())) {
            name += " " + user.getLastName();
        }*/
        return StrUtil.subPre(name, 20);
    }

    public static String tgName(WodiTop user) {
        String name = user.getFirstName();
        /*if (StrUtil.isNotBlank(user.getLastName())) {
            name += " " + user.getLastName();
        }*/
        return StrUtil.subPre(name, 20);
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
        jsonObject.put("action", GameEventUtil.ACTION_JOIN_GAME);
        joinGame.setCallbackData(jsonObject.toString());
        List<InlineKeyboardRow> rows = CollUtil.newArrayList();
        rows.add(new InlineKeyboardRow(joinGame));
        if (startButton) {
            InlineKeyboardButton readyGame = new InlineKeyboardButton(READY);
            JSONObject readyJn = new JSONObject();
            readyJn.put("action", GameEventUtil.ACTION_READY);
            readyGame.setCallbackData(readyJn.toString());

            InlineKeyboardButton exitGame = new InlineKeyboardButton(EXIT);
            JSONObject exitJn = new JSONObject();
            exitJn.put("action", GameEventUtil.ACTION_EXIT);
            exitGame.setCallbackData(exitJn.toString());

            InlineKeyboardRow rowInline2 = new InlineKeyboardRow(readyGame, exitGame);
            if (WdUtil.isAllMemberReady(game)) {
                InlineKeyboardButton openGame = new InlineKeyboardButton(START);
                JSONObject openJn = new JSONObject();
                openJn.put("action", GameEventUtil.ACTION_OPEN);
                openGame.setCallbackData(openJn.toString());
                rowInline2.add(openGame);
            }
            rows.add(rowInline2);
        }
        return new InlineKeyboardMarkup(rows);
    }

    /**
     * 获得投票菜单
     *
     * @param game 游戏
     * @return {@link InlineKeyboardMarkup }
     */
    public static InlineKeyboardMarkup getVoteMarkup(Game game) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        // 用于跟踪当前行的按钮数量
        int counter = 0;
        InlineKeyboardRow rowInline = new InlineKeyboardRow();

        for (GameUser member : game.memberList) {
            if (member.survive) {
                InlineKeyboardButton button = new InlineKeyboardButton(TgUtil.tgName(member.user));
                JSONObject data = new JSONObject();
                data.put("action", GameEventUtil.ACTION_VOTE);
                data.put("to", member.id);
                button.setCallbackData(data.toString());
                rowInline.add(button);
                counter++;

                // 如果当前行有两个按钮，或者是最后一个按钮，添加到rows并重置行
                if (counter == 2) {
                    rows.add(rowInline);
                    rowInline = new InlineKeyboardRow();
                    counter = 0;
                }
            }
        }
        // 如果最后一行有未添加的按钮，确保它们被添加
        if (!rowInline.isEmpty()) {
            rows.add(rowInline);
        }

        rowInline = new InlineKeyboardRow();
        InlineKeyboardButton button = new InlineKeyboardButton(abstain);
        JSONObject data = new JSONObject();
        data.put("action", GameEventUtil.ACTION_VOTE);
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
        jsonObject.put("action", GameEventUtil.ACTION_VIEW_WORD);
        viewWord.setCallbackData(jsonObject.toString());
        viewWord.setUrl("t.me/" + botUserName);
        List<InlineKeyboardRow> rows = CollUtil.newArrayList();
        rows.add(new InlineKeyboardRow(viewWord));
        return new InlineKeyboardMarkup(rows);
    }

    /**
     * 生成惊喜盒子
     *
     * @param def 初始数量
     * @return {@link InlineKeyboardMarkup }
     */
    public static InlineKeyboardMarkup getSbBtn(Integer def) {
        if (null != def) {
            SB_BOX_CNT.set(def);
        } else {
            def = SB_BOX_CNT.decrementAndGet();
        }
        InlineKeyboardButton viewWord = new InlineKeyboardButton(StrUtil.format("狠狠点击我 -> {}", def));
        viewWord.setCallbackData(JSONObject.of("action",
                StrUtil.format(GameEventUtil.PUBLIC_ACTION_SB)).toString());
        List<InlineKeyboardRow> rows = CollUtil.newArrayList();
        rows.add(new InlineKeyboardRow(viewWord));
        return new InlineKeyboardMarkup(rows);
    }

    /**
     * 红包按钮
     *
     * @param redId 红包id
     * @return {@link InlineKeyboardMarkup }
     */
    public static InlineKeyboardMarkup getRedBtn(String redId) {
        InlineKeyboardButton viewWord = new InlineKeyboardButton("运气爆棚 🎁");
        viewWord.setCallbackData(JSONObject.of("action",
                GameEventUtil.PUBLIC_ACTION_RED + redId).toString());
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
            preJn.put("action", GameEventUtil.PUBLIC_ACTION_RANKS + ":" + (currentPage - 1));
            prevButton.setCallbackData(preJn.toString());
            pageBtn.add(prevButton);
        }

        if (currentPage < totalPages) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton("下一页 ➡️");
            JSONObject nextJn = new JSONObject();
            nextJn.put("action", GameEventUtil.PUBLIC_ACTION_RANKS + ":" + (currentPage + 1));
            nextButton.setCallbackData(nextJn.toString());
            pageBtn.add(nextButton);
        }
        rowList.add(pageBtn);

        // 添加关闭按钮
        /*InlineKeyboardButton closeButton = new InlineKeyboardButton("❌ - 关闭");
        closeButton.setCallbackData("closeit");
        rowList.add(new InlineKeyboardRow(closeButton));*/

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
        return CollUtil.getFirst(StrUtil.splitTrim(
                StrUtil.removeAll(msgData, StrUtil.AT + botUserName),
                StrUtil.SPACE)).toLowerCase();
    }

    /**
     * 是群消息
     *
     * @param callbackQuery 消息
     * @return boolean
     */
    public static boolean isGroupMsg(CallbackQuery callbackQuery) {
        MaybeInaccessibleMessage message = callbackQuery.getMessage();
        return message.isGroupMessage() || message.isSuperGroupMessage();
    }

    /**
     * 是群消息
     *
     * @param message 消息
     * @return boolean
     */
    public static boolean isGroupMsg(Message message) {
        return message.isGroupMessage() || message.isSuperGroupMessage();
    }

    /**
     * 是群消息
     *
     * @param message 消息
     * @return boolean
     */
    public static boolean isPrivateMsg(Message message) {
        return !isGroupMsg(message);
    }

    /**
     * 消息是否有效
     *
     * @param msg 味精
     * @return boolean
     */
    public static boolean isMessageValid(Message msg) {
        if (null == msg || !msg.hasText() || null != msg.getForwardDate()) {
            return false;
        }
        if (System.currentTimeMillis() / 1000 - msg.getDate() > 60) {
            log.warn("过期指令:【{}】{}", msg.getFrom().getFirstName(), msg.getText());
            return false;
        }
        return true;
    }

    /**
     * 用户加入消息
     *
     * @param msg 味精
     * @return boolean
     */
    public static boolean isNewMember(Message msg) {
        if (null == msg || CollUtil.isEmpty(msg.getNewChatMembers())) {
            return false;
        }
        return true;
    }

    /**
     * 用户离开消息
     *
     * @param msg 味精
     * @return boolean
     */
    public static boolean isLeftMember(Message msg) {
        if (null == msg || null == msg.getLeftChatMember()) {
            return false;
        }
        return true;
    }

    /**
     * 用户离开消息
     *
     * @param chatMember 味精
     * @return boolean
     */
    public static boolean isAdmin(ChatMember chatMember) {
        if (null == chatMember) {
            return false;
        }
        return StrUtil.equalsAny(chatMember.getStatus(), "creator", "administrator");
    }

    public static void main(String[] args) {
        Console.log(extractCommand("/wd_rank@WorldLineGame_bot 123", "WorldLineGame_bot"));
        Console.log(extractCommand("/wd_rank@WorldLi", "WorldLineGame_bot"));
        Console.log(extractCommand("/wd_rank 123", "WorldLineGame_bot"));
    }
}