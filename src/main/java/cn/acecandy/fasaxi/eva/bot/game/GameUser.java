package cn.acecandy.fasaxi.eva.bot.game;

import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.acecandy.fasaxi.eva.utils.GameUtil;
import lombok.Data;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author tangningzhu
 * @since 2025/1/10
 */
@Data
public class GameUser {
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
    @Getter
    public boolean survive = true;
    /**
     * 没有投票
     */
    public int notVote = 0;

    /**
     * 本轮投票时间
     */
    public long voteTime = Long.MAX_VALUE;

    /**
     * 存活回合
     */
    public int round = 0;
    /**
     * 投票给
     */
    public GameUser toUser;
    /**
     * 游戏结算分
     */
    public int fraction = 0;
    public int dmailUp = 0;

    public boolean speak = false;
    public String boom = "";

    public GameUser(User user, WodiUser wodiUser) {
        this.user = user;
        this.id = user.getId();
        this.wodiUser = wodiUser;
        this.oldLevel = GameUtil.levelByScore(wodiUser.getFraction());
    }
}