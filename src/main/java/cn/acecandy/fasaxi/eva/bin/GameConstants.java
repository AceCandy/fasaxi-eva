package cn.acecandy.fasaxi.eva.bin;


/**
 * 游戏时间常量类
 *
 * @author AceCandy
 * @since 2024/10/16
 */
public interface GameConstants {
    /**
     * 最大玩家数量
     */
    Integer MAX_PLAYER = 10;
    /**
     * 发送加入游戏邀请时间间隔
     */
    long WaitingYoJoinTimeInterval = 1000 * 30;
    /**
     * 最小开始游戏所需人数
     */
    long minMemberSize = 4;
    /**
     * 超时关闭游戏时间 （仅在游戏未开始有效)
     */
    long MaxActiveTime = 1000 * 60;
    /**
     * 讨论时间每位玩家增加的游戏(秒数 )
     */
    long GameSecondsAddedByThePlayer = 20;
    /**
     * 设置最大讨论时间上限 (秒数 )
     */
    long DiscussionTimeLimit = 150;
    long DiscussionTimeLimitMin = 60;
    /**
     * 设置最大投票时间上限
     */
    long voteTimeLimit = 1000 * 60;
    /**
     * 设置最大未投票自动淘汰上限
     */
    int notVote = 2;
    /**
     * 淘汰所需的最大票数,
     */
    int MAXIMUM_VOTE = 4;
    /**
     * 提醒发言倒计时
     */
    int voteReminderVote = 20 * 1000;
    /**
     * 参与分数
     */
    int joinScore = 5;
    /**
     * 参与分数
     */
    int spyJoinScore = 6;
    /**
     * 平民获胜得分
     */
    int peopleVictoryScore = 2;
    /**
     * 每 8人卧底获胜+分
     */
    int spyVictoryScore = 2;
    /**
     * 白板胜利+分
     */
    int spaceVictoryScore = 3;
    /**
     * 淘汰扣分
     */
    int faileScore = -3;

}