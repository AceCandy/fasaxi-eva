package cn.acecandy.fasaxi.eva.bin;


/**
 * 常量类
 *
 * @author AceCandy
 * @since 2024/10/16
 */
public interface Constants {
    String TIP_IN_GROUP = "请在群组中使用命令~";
    String TIP_IN_OWNER = "您无法使用该命令~";
    String TIP_IN_RANK = "{} 花费20封Dmail进行开启榜单";
    String TIP_HELP = """
                      使用命令可创建游戏（<b>花费10封Dmail</b>），游戏开始后每个人会得到自己的游戏词语。
                      
                      1. 房主开启游戏后才扣取Dmail，未成功开局不扣除。
                      2. 阵营获胜房主积分底分+1（享受成就加成），获得12封Dmail。阵营失败房主积分底分+2，回收6封Dmail。
                      3. 分为平民/卧底两个阵营，大部分玩家会获得平民词，少部分获会得卧底词。
                      4. 大家需要描述自己的词语，但是<b>不能直接说出该词语</b>。
                      5. 第一轮由<b>系统通知</b>首个玩家先进行发言描述，后续无顺序要求。
                      
                      <b>小tips:</b>
                      开局时大家<b>并不知道自己是否卧底</b>，需要根据他人的描述来判定，多数一致的词一般为平民阵营。
                      每位平民玩家需要描述自己的词语来证明自己不是卧底，但是尽量在<b>不让卧底猜到自己的词</b>的同时找到平民队友。
                      拿到卧底词语的玩家同样需要证明自己不是卧底，<b>尽快根据场上描述猜到平民词伪装混进平民阵营</b>。
                      
                      <b>游戏限制</b>：游戏最多10人参与，每轮至少45秒参与投票。
                      <b>胜利条件</b>：平民<b>找出所有卧底</b>时平民胜利。当<b>场上只剩下1名卧底和1名平民</b>时，卧底获得胜利。
                      
                      /wodi 创建游戏
                      /wodi_record 查看自身记录
                      /wodi_rank 查看积分排行榜（20Dmail）
                      /wodi_top 查看飞升第一人榜单（20Dmail）
                      /wodi_exit 退出游戏
                      """;
    String userCreateGame = "{} 花费10封Dmail（成功开局后才扣取），创建了一个新游戏";
    String RECORD_TXT = """
                        \n
                        🍒 <b>用户名</b>: {userName}
                        🍌 <b>游戏场次</b>：{completeGame} ({total_percentage})
                        🍎 <b>平民场次</b>：{word_people_victory} / {word_people} ({people_percentage})
                        👿 <b>卧底场次</b>：{word_spy_victory} / {word_spy} ({spy_percentage})
                        🏆 <b>等级积分</b>：<b>{level}</b>（{fraction}）
                        🐉 <b>特权</b>：<b>无加成</b>
                        """;
    String RANK = "\n<b>▎🏆 [谁是卧底]  无限远点的牵牛星 ♪</b>\n\n";
    String TOP_TITLE = "\n<b>▎🏆 [谁是卧底]  闭时曲线的碑文 ♪</b>\n\n";
    String InTheGame = "{},这个群组正在游戏中";
    String JOIN_GAME = "加入游戏";
    String GamePlayerWaiting = """
                               【<b>谁是卧底</b>】游戏开启，等待玩家进入
                               
                               当前已加入{}人: {}
                               """;
    String READY = "准备";
    String EXIT = "退出";
    String START = "开始";
    String TimeoutShutdown = "{}长时间无操作，游戏已自动关闭！";
    String sendWord = """
                            你在<b>{}</b>群组中的游戏词语是: <b>{}</b>
                      """;
    String exitGame = "{} 强行关闭了游戏";
    String SPEECH_TIME = """
                         当前存活人: {}
                         接下来是发言时间，在进行描述的时候请加上<b>‘，’中文逗号前缀</b>，才算一次有效发言。
                         <b>{}</b>秒后将开始第<b>{}</b> 轮投票。\n
                         {}
                         """;
    String firstSpeak = "请由<b>{}</b>先进行发言";
    String ViewWord = "查看词语";
    String gameStart = "房主{}扣除10封Dmail成功！\n所有玩家准备就绪，游戏初始化中\n\n";
    String votingStart = "现在开始投票，你想淘汰谁？";
    String notVoteSelf = "❌ 不能投自己";
    String notSpeakVote = "❌ 未发言玩家禁止投票";
    String abstain = "放弃投票 🏳️";
    String success = "✅ 成功";
    String OPERATED_BEFORE = "❌ 已经操作过了";
    String failure = "失败";
    String ABSTAINED = "{} 放弃了这一票";
    String NOT_VOTE = "{} 没有在时间内进行投票";
    String votedTimeEnd = "️⌛️投票时间到：";
    String everyoneVoted = "✅ 所有人都完成了投票：";
    String GAME_OVER = "🎇 游戏结束 <b>{}</b> 胜利！！！🎇\n";
    String DIVIDING_LINE = "------------------------\n";
    String GAME_OVER_BOOM_UNDERCOVER = "<b>🤡卧底 阵营达成【全员恶人你怕了吗】成就，奖励积分翻倍！！</b>\n";
    String GAME_OVER_BOOM_PEOPLE = "<b>👨‍🌾平民 阵营达成【坚决肃清黑恶势力】成就，奖励积分翻倍！！</b>\n";
    String GAME_OVER_BOOM_SINGLE_UNDERCOVER = "<b>🤡卧底 阵营达成【白板怎么能算我们的人】成就，奖励积分+3！</b>\n";
    String GAME_OVER_BOOM_SINGLE_UNDERCOVER2 = "<b>{}刺客 达成【我是荒原上一匹孤独的狼】成就，奖励积分+5！</b>\n";
    String GAME_OVER_BOOM_SINGLE_PEOPLE = "<b>👨‍🌾平民 阵营达成【兄弟，还得是你】成就，奖励积分+5！</b>\n";
    String GAME_OVER_BOOM3 = "<b>{}屠夫 达成【哈哈哈哈哈，都没想到吧】成就，奖励积分三倍！！！</b>\n";
    String aboutToVoteL = "即将开始投票, 倒计时{}s, ";
    String aboutToVoteR = "还没有发言";
    String notAdmin = """
                      <i>没有看到有人说话，我可能没有权限</i>
                      """;
    String USER_WORD_IS = "{} 的词语是 【{}】";
    String USER_DMAIL = "🎉 恭喜[Lv{}]{}本次赢得 <b>{}</b> 封Dmail \n";
    String USER_DMAIL_OWNER_WIN = "🎉💓 房主{}阵营获胜！奖励 <b>{}</b> 封Dmail \n";
    String USER_DMAIL_OWNER_FAIL = "🎉☠️ 房主{}阵营失败！回收 <b>{}</b> 封Dmail \n";
    String USER_FULL = "💓 参加人数达到{}人！全体增加 <b>{}</b> 封Dmail \n";
    String RORATE_FULL = "💓 激战回合数达到{}轮！全体增加 <b>{}</b> 封Dmail \n";
    String USER_LEVEL_UP = "🚀 祝贺{} 飞升为 <b>【{}】</b>，收获礼包 <b>{}</b> 封Dmail！\n";
    // String USER_LEVEL_UP_FIRST = "🏆🚀🚀🚀 {} 成为<b>首个</b>飞升至 <b>【{}】</b>的超级大牛, 额外收获 <b>{}</b> 封Dmail！大家快来膜拜他！！！\n";
    String USER_LEVEL_UP_FIRST = """
                                 🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨
                                 🚨
                                 🚨  🏆 {}成为<b>【{}】</b>飞升第一人
                                 🚨  💰 收获<b>{}</b>封Dmail！{}
                                 🚨  💓 <b>群内所有成员Dmail+5</b>
                                 🚨  🎉 大家恭喜这个b！！！！！
                                 🚨
                                 🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨
                                 """;
    String USER_LEVEL_UP_HIGH = "注册码<b>{}</b>个";
    String killUserWordIs = "<s>{}</s> 的词语是 【{}】";
    String ELIMINATED_IN_THIS_ROUND = "💀 第{}轮，本轮淘汰：";
    String GAME_SETTLEMENT = "游戏正在结算中，请稍候";
    String remainingPersonnel = "剩余人员({}/{}):\n{}";

    String RIGISTER_TIPS = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_Y7OE1csLqg\n";
    String RIGISTER_CODE1 = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_Y7OE1csLqg\n";
    String RIGISTER_CODE2 = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_3xl3qhhig0\n";
    String RIGISTER_CODE3 = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_5VoOWFteXV\n";
    String RIGISTER_CODE4 = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_qBZaZ6NEIP\n";
    String RIGISTER_CODE5 = """
                            t.me/WorldLineEmby_bot?start=WorldLine-30-Register_5CL4LdTaYL
                            t.me/WorldLineEmby_bot?start=WorldLine-30-Register_fuhUWPnxjL
                            t.me/WorldLineEmby_bot?start=WorldLine-30-Register_p900zgDCBG
                            """;
}