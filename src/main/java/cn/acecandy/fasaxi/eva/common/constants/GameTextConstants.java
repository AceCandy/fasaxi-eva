package cn.acecandy.fasaxi.eva.common.constants;


/**
 * 常量类
 *
 * @author AceCandy
 * @since 2024/10/16
 */
public interface GameTextConstants {
    /**
     * 当前赛季
     */
    Integer CURRENT_SEASON = 2;

    String NO_AUTH_GROUP = "❌ 严重警告，未授权群组不允许使用本Bot，当前已被发现！当前群组信息已上传云端！";
    String CURFEW_GAME_TIME = "❌ 当前未在可游戏时间，限制可游戏时间10～22点(剔除吃饭午休时间)！";
    String TIP_IN_GROUP = "请在群组中使用命令~";
    String TIP_IN_OWNER = "您无法使用该命令~";
    String NO_EMBY_USER_TIP = "❌ 当前未在bot开号！";
    String TIP_IN_RANK = "您花费15封Dmail进行开启榜单";
    String TIP_IN_TOP = "您花费10封Dmail进行开启榜单";
    String TIP_HELP = """
                      使用命令可创建游戏（<b>花费10封Dmail 成功开局才扣除</b>），游戏开始后每个人会得到自己的游戏词语。
                      
                      <b>玩法小tips:</b>
                      1. 分为平民/卧底两个阵营，大部分玩家会获得平民词，少部分获会得卧底词。
                      2. 开局时大家<b>并不知道自己是否卧底</b>，需要根据他人的描述来判定，多数一致的词为平民阵营。
                      3. 每位平民玩家需要描述自己的词语来证明自己不是卧底，但是尽量在<b>不让卧底猜到自己的词</b>的同时找到平民队友。
                      4. 拿到卧底词语的玩家同样需要证明自己不是卧底，<b>尽快根据场上描述猜到平民词伪装混进平民阵营</b>。
                      5. 没有拿到词的称为白板，白板属于卧底阵营，尽量伪装并混入并帮助卧底赢得胜利吧。
                      6. 某些特殊模式下，按照人多的阵营属于平民这个规则来说，白板会属于平民方，所以不要被词所限制。
                      
                      <b>规则说明:</b>
                      1. <b>游戏限制</b>：游戏最多10人参与，通过发言描述自己的词语，所有人发言完成后进入投票环节。对内包含平票或高票不超过全对存活人数1/3则继续，否则最高票被淘汰出局。重复轮次直到达成一方胜利条件。
                      2. <b>胜利条件</b>：平民<b>找出所有卧底</b>时平民胜利。当<b>场上只剩下1名平民</b>时，如果卧底存在，则卧底获得胜利。
                      3. <b>白板附加胜利条件（特殊模式无效）</b>：白板在<b>除第一轮外</b>的每一轮的讨论环节（自己未发言状态下）可以通过“，。XXX”（前缀两个标点均为中文标点）进行猜平民词，猜词成功则直接获胜，猜词失败则自己暴毙(扣除2Dmail)，游戏继续。此方式威力极大且人畜不分，队友也杀，慎用！
                      4. <b>发言顺序</b>：第一轮由<b>系统通知</b>前两名玩家进行发言描述，后续无顺序要求。
                      5. <b>发言规则</b>：通过“，XXX”（中文逗号前缀）此种方式正常描述自己的词，但是禁止无发言、本轮重复发言（扣除2Dmail），另外严禁包括谐音等方式爆本家词（扣除5Dmail）
                      6. <b>投票规则</b>：有投票权的玩家需要在规定时间内投票，未发言玩家无法参与投票环节，两次不参与投票（弃票不算）的玩家将会被系统kill（扣除5Dmail）。
                      7. <b>房主优劣</b>：阵营获胜房主积分底分+1（享受成就加成），获得12封Dmail。阵营失败房主积分底分+2，回收6封Dmail。
                      8. <b>积分成就</b>：每把游戏其中无论输赢游戏积分均会增长，Dmail则根据胜负情况增减。根据游戏情况积分的获取多少有不同，获得成就会有额外的积分奖励。短时间内Dmail减少很正常，不必惊慌，尽量多获得积分才重要。
                      9. <b>飞升规则</b>：游戏积分达到一定数值后可以飞升，飞升后可以获得大量Dmail奖励，并能提供Dmail增益Buff。
                      
                      /wd 创建游戏（10Dmail 成功开局后扣取）
                      /wd_info 查看自身游戏积分记录（2Dmail）
                      /wd_rank 翻阅游戏积分排行榜（15Dmail）
                      /wd_top 翻阅首飞霸王榜（10Dmail）
                      /wd_exit 关闭游戏（3Dmail）
                      """;
    String SPEAK_TIME_LIMIT = "当前还差<b>{}</b>条发言才可以开启游戏哦🤣！";
    String userCreateGame = "{} 花费10封Dmail（成功开局后才扣取），创建了一个新游戏";
    String RECORD_TXT = """
                        \n
                        ◎ <b>{level}</b> {userName} ({fraction})
                        
                        🎮 <b>竞技场次</b>：{completeGame} ({total_percentage})
                        👨‍🌾 <b>平民场次</b>：{word_people_victory} / {word_people} ({people_percentage})
                        🤡 <b>卧底场次</b>：{word_spy_victory} / {word_spy} ({spy_percentage})
                        💰 <b>流通货币</b>：{dm}<b> (无加成)</b>
                        """;
    String RANK = "\n<b>▎🏆 [谁是卧底] 无限远点的牵牛星 S{}赛季 ♪</b>\n\n";
    String TOP_TITLE = "\n<b>▎🏆 [谁是卧底] {} S{}赛季 ♪</b>\n\n";
    String IN_GAMING = "{},这个群组正在游戏中";
    String JOIN_GAME = "加入游戏";
    String GAME_WAITING = """
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
    String EXIT_GAME = "{} 花费3Dmail强行关闭了游戏";
    String EXIT_GAME_ERROR = "❌ 只有房主可以关闭游戏";
    String SPEECH_TIME = """
                         当前存活人: {}
                         接下来是发言时间，在进行描述的时候请加上<b>‘，’中文逗号前缀</b>，才算一次有效发言。
                         <b>{}</b>秒后将开始第<b>{}</b> 轮投票。\n
                         {}
                         """;
    String SPEAK_ORDER = "请由 <b>{}</b> 先进行首位发言；\n<b>{}</b> 进行第二位发言";
    String ViewWord = "查看词语";
    String GAME_START = "房主{}扣除10封Dmail成功！\n所有玩家准备就绪，游戏初始化中\n\n";
    String VOTING_START = "现在开始投票，你想淘汰谁？";
    String NOT_VOTE_SELF = "❌ 不能投自己";
    String NO_SPEAK_NO_VOTE = "❌ 未发言玩家禁止投票";
    String abstain = "放弃投票 🏳️";
    String VOTE_SUCCESS = "✅ 投票成功";
    String VOTE_FINISH = "❌ 已经投票完成";
    String failure = "失败";
    String VOTE_PUBLICITY = "{} 👉 [{}]\n";
    String VOTE_ABSTAINED = "{} 放弃了这一票\n";
    String NOT_VOTE = "{} 没有在时间内进行投票\n";
    String ANONYMOUS_VOTE = "️🎭 由于不可抗力发生，本轮将匿名不进行公布";
    String TIME_END_VOTED = "️⌛️投票时间到，投票结束！\n";
    String ALL_FINISH_VOTED = "✅ 所有人都完成了投票！\n";
    String LAST_VOTE = "👀 本轮最后投票人: {}\n\n";
    String GAME_OVER = "🎇 {}游戏结束 <b>{}</b> 胜利！！！🎇\n";
    String DIVIDING_LINE = "------------------------\n";
    String GAME_OVER_BOOM_SPACE = "<b>🀫 白板 达成【只能孤注一掷了不然我怎么活】成就，奖励积分+4！！</b>\n";
    String GAME_OVER_BOOM_SPACE2 = "<b>🀫 白板 达成【对不起我是个警察】成就，奖励积分-1！！</b>\n";
    String GAME_OVER_BOOM_SPACE3 = "<b>🀫 白板 达成【兄弟我不敢赌】成就，奖励积分+5！！</b>\n";
    String GAME_OVER_BOOM_UNDERCOVER = "<b>🤡卧底 阵营达成【全员恶人你怕了吗】成就，奖励积分翻倍！！</b>\n";
    String GAME_OVER_BOOM_PEOPLE = "<b>👨‍🌾平民 阵营达成【坚决肃清黑恶势力】成就，奖励积分1.5倍！！</b>\n";
    String GAME_OVER_BOOM_PEOPLE_SPECIAL = "<b>👨‍🌾平民 阵营达成【空白的世界】成就，奖励积分翻倍！！</b>\n";
    String GAME_OVER_BOOM_SINGLE_UNDERCOVER = "<b>🤡卧底 阵营达成【白板怎么能算我们的人】成就，奖励积分+3！</b>\n";
    String GAME_OVER_BOOM_SINGLE_UNDERCOVER2 = "<b>🤡卧底 达成【我是荒原上的一匹孤狼】成就，奖励积分+5！</b>\n";
    String GAME_OVER_BOOM_SINGLE_PEOPLE = "<b>👨‍🌾平民 阵营达成【兄弟，不愧是你】成就，奖励积分+5！</b>\n";
    String GAME_OVER_BOOM3 = "<b>{}屠夫 达成【哈哈哈哈哈，都没想到吧】成就，奖励积分三倍！！！</b>\n";
    String GAME_OVER_BOOM3_SPECIAL = "<b>{}屠夫 达成【神不在的日子】成就，奖励积分三倍！！！</b>\n";
    String VOTE_COUNT_DOWN = "即将开始投票, 倒计时{}s, ";
    String aboutToVoteR = "还没有发言";
    String notAdmin = """
                      <i>没有看到有人说话，我可能没有权限</i>
                      """;
    String USER_WORD_IS = "{} 的词语是 【{}】";
    String KILL_USER_WORD_IS = "<s>{}</s> 的词语是 【{}】";
    // String killUserWordIs = "<s>{}</s> 的词语是 【{}】";
    String USER_DMAIL = "🎉 恭喜[Lv{}]{}本次赢得 <b>{}</b> 封Dmail \n";
    String USER_DMAIL_OWNER_WIN = "🚩💓 房主{}阵营获胜！奖励 <b>{}</b> 封Dmail \n";
    String USER_DMAIL_OWNER_FAIL = "🚩☠️ 房主{}阵营失败！回收 <b>{}</b> 封Dmail \n";
    String USER_FULL = "✅ 参加人数达到{}人！全体增加 <b>{}</b> 封Dmail \n";
    String RORATE_FULL = "✅ 激战回合数达到{}轮！全体增加 <b>{}</b> 封Dmail \n";
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
    String SEASON_ENDS = """
                         🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨
                         🚨  🎇 游 戏 通 关，赛 季 结 束！🎇
                         🚨
                         🚨  🏆 {}成为<b>【{}】</b>飞升第一人
                         🚨  💰 收获<b>{}</b>封Dmail！{}
                         🚨  💓 <b>群内所有成员Dmail+10</b>
                         🚨  🎉 大家恭喜这个b！！！！！
                         🚨
                         🚨  <b>新赛季即将开始</b>，当前进入季后赛阶段！
                         🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨
                         """;
    String USER_LEVEL_UP_HIGH = "注册码<b>{}</b>个";

    String ELIMINATED_IN_THIS_ROUND = "💀 第{}轮，本轮淘汰：";
    String GAME_SETTLEMENT = "游戏正在结算中，无法退出";
    String SURVIVAL_PERSONNEL = "\n剩余人员({}/{}):\n{}";
    String RUN_AWAY_QUICKLY = "❗️{} 抢跑发言位，此次发言无效并扣除5封Dmail🫶\n";
    String NOT_VOTED_TIP = "❗️{} 两轮连续跑路，扣除5封Dmail🫶\n";
    String SPEAK_REPEAT = "❓️{} 无发言或重复发言，此次发言无效并扣除2封Dmail🫶\n";
    String SPEAK_NOWAY = "❓️{} 违禁爆词，扣除5封Dmail🫶\n";

    String RIGISTER_TIPS = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_Y7OE1csLqg\n";
    String RIGISTER_CODE1 = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_Y7OE1csLqg\n";
    String RIGISTER_CODE2 = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_3xl3qhhig0\n";
    String RIGISTER_CODE3 = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_5VoOWFteXV\n";
    String RIGISTER_CODE4 = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_qBZaZ6NEIP\n";
    String RIGISTER_CODE5 = """
                            t.me/WorldLineEmby_bot?start=WorldLine-30-Register_5CL4LdTaYL
                            """;
    String RIGISTER_CODE = "t.me/WorldLineEmby_bot?start=WorldLine-30-Register_fuhUWPnxjL";
    // t.me/WorldLineEmby_bot?start=WorldLine-30-Register_fuhUWPnxjL
    // t.me/WorldLineEmby_bot?start=WorldLine-30-Register_p900zgDCBG

    String BOOM_WAITING = "🌌🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪\n🌌\n🌌嘘～噤声！神秘的屠夫悄悄从屁股中缓缓地抽出了五米长的钢刀…………\n🌌\n🌌🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪";
    String BOOM_FAIL = "☠️❌☠️❌☠️ 玩家【{}】突然暴毙，没人知道发生了没什么（并且丢失了2Dmail）。剩余玩家游戏继续…………";

    String SEASON0 = "零化域的缺失之环";
    String SEASON1 = "闭时曲线的碑文";
    String SEASON2 = "双体福音的契约";
    String SEASON3 = "永劫回归的潘多拉";
    String SEASON4 = "私密镜里的圣痕";
    String SEASON5 = "亡失流转的孤独";
    String SEASON6 = "轨道秩序的暗蚀";
    String SEASON7 = "存在证明的自动人偶";
    String SEASON8 = "二律背反的双重人格";
    String SEASON9 = "相互再归的鹅妈妈";
    String SEASON10 = "盟誓的文艺复兴";
    String SEASON11 = "无限远点的牵牛星";
    String SEASON12 = "交叉坐标的星辰";
}