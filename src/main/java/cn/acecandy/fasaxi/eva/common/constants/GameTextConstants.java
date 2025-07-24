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
    Integer CURRENT_SEASON = 3;

    String NO_AUTH_GROUP = "❌ 严重警告，未授权群组不允许使用本Bot，当前群组信息已上传网信部记录！";
    String CURFEW_GAME_TIME = "❌ 当前未在可游戏时间，游戏开放时间9～12、14～18、19～22点！";
    String TIP_IN_GROUP = "请在群组中使用命令~";
    String TIP_IN_PRIVATE = "请私聊bot使用命令~";
    String TIP_IN_OWNER = "您无法使用该命令~";
    String TIP_IN_XRENEW_CREATE = "❌ 指令不正确，格式参考： /xm_create [Dmail] [数量]";
    String TIP_IN_XRENEW_USE = "❌ 指令或码子格式不正确，格式参考： /xm_use [code]";
    String TIP_IN_XRENEW_USED = "❌ 码子不存在或者已经使用过了，请勿频繁操作";
    String XRENEW_USE_ERROR = "❌ db更新失败，请联系腐竹";
    String XRENEW_USE_SUCC = "✅ 使用码子[{}]成功，恭喜您获得了 {} 封Dmail！";
    String XRENEW_CREATE_ERROR = "❌ 创建续命码失败";
    String XRENEW_CREATE_SUCC = "✅ 成功创建{}个{}Dmail续命码";
    String NO_EMBY_USER_TIP = "❌ 当前未在bot开号！";
    String TIP_IN_INVITE = "您花费{}封Dmail创建传承邀请";
    String TIP_IN_RANK = "您花费{}封Dmail开启榜单";
    String TIP_IN_CHECKIN = "您花费{}封Dmail开启个人信息";
    String TIP_IN_CHECKIN_NOPAY = "已开启过个人信息，3分钟内免除查询消费";
    String TIP_HELP = """
                      使用命令可创建游戏（<b>花费10封Dmail 成功开局才扣除</b>），游戏开始后每个人会得到自己的游戏词语。
                      
                      <b>玩法小tips:</b>
                      1. 玩家分为平民/卧底两个阵营，获取词语占多数的为平民词，占少数的为卧底。开局时大家<b>并不知道自己属于哪一方</b>，需要根据他人的描述来判定。
                      2. 玩家需要多轮描述自己的词语来判别自己所在的阵营。玩家通过场上描述判别自己为平民方还是卧底方，如果是平民方：尽量在<b>不让卧底猜到自己的词</b>的同时找到平民队友；如果是卧底方：<b>尽快根据场上描述猜到平民词伪装混进平民阵营</b>
                      3. 没有词的称为白板，白板属于卧底阵营，尽量伪装并混入并帮助卧底赢得胜利吧。
                      4. 特殊模式下，按照人多的阵营属于平民这个规则来说，白板会属于平民方，所以不要被词所限制。
                      
                      <b>规则说明:</b>
                      1. <b>游戏限制</b>：游戏最多10人参与，通过发言描述自己的词语，所有人发言完成后进入投票环节。对内包含平票或高票不超过全对存活人数1/3则继续，否则最高票被淘汰出局。重复轮次直到达成一方胜利条件。
                      2. <b>胜利条件</b>：平民<b>找出所有卧底</b>时平民胜利。当<b>场上只剩下1名平民</b>时，如果卧底存在，则卧底获得胜利。
                      3. <b>白板附加胜利条件（特殊模式无效）</b>：非异常对局中，白板在<b>除第一轮外</b>的每一轮的讨论环节（自己未发言状态下）可以通过“，。XXX”（前缀两个标点均为中文标点）进行猜平民词，猜词成功则直接获胜，猜词失败则自己暴毙(扣除2Dmail)，游戏继续。此方式威力极大且人畜不分，队友也杀，慎用！
                      4. <b>发言顺序</b>：第一轮由<b>系统通知</b>前两名玩家进行发言描述，后续无顺序要求。
                      5. <b>发言规则</b>：通过“，XXX”（中文逗号前缀）此种方式正常描述自己的词，但是禁止无发言、本轮重复发言（罚币），另外严禁各种方式爆本家词（罚币）
                      6. <b>投票规则</b>：有投票权的玩家需要在规定时间内投票，未发言玩家无法参与投票环节，连续不参与投票或者多次弃投的玩家将会被系统kill（罚币）。
                      7. <b>房主优劣</b>：房主开局需花费10Dmail，胜利收获14Dmail，失败收获7Dmail。
                      8. <b>积分成就</b>：每把游戏其中无论输赢游戏积分均会增长，Dmail则根据胜负情况增减。达成成就会有额外的积分奖励。
                      9. <b>等级飞升</b>：积分达到固定阈值后升级，升级降获得大量Dmail奖励和buff增益。
                      9. <b>战力提升</b>：优秀场次可以提升战力，排位前19均能享受buff增益。所以以战力为王多多获取战力值的提升吧！
                      10. <b>境界霸主</b>：战力累计优先达到阈值能晋升段位霸主名列top榜单，获得大量Dmail和buff增益，每个段位只有一次机会。
                      
                      /wd 创建游戏(10 Dmail)
                      /wd_checkin 签到并查看个人信息(5 Dmail 3分钟内无消耗)
                      /wd_rank 开启积分榜(10 Dmail)
                      /wd_real_rank 开启实时战力榜(10 Dmail)
                      /wd_top 开启登顶霸王榜(3 Dmail)
                      /wd_exit 关闭游戏(3 Dmail)
                      
                      PS：无Dmail的情况均以Email进行替代
                      """;
    String SPEAK_TIME_LIMIT = "当前还差<b>{}</b>条发言才可以开启游戏哦🤣！";
    String userCreateGame = "{} 花费10封Dmail（成功开局后才扣取），创建了一个新游戏";
    String RECORD_TXT = """
                        \n
                        ††††††††††††††††††††††††
                        | 📍 境界：<b>{level}</b>
                        | 🔹 {userName}
                        | 🧊 战力：<b>{power}</b>（排名：{rankIndex}）
                        ††††††††††††††††††††††††
                        
                        🎮 <b>竞技积分</b>：{fraction} / {completeGame}场
                        🤵 <b>平民场次</b>：{word_people_victory} / {word_people} ({people_percentage})
                        ⛄ <b>卧底场次</b>：{word_spy_victory} / {word_spy} ({spy_percentage})
                        💎 <b>流通货币</b>：{dm}
                        🎖️ <b>额外加成</b>：无加成
                        🕯️ <b>头衔</b>：无头衔
                        
                        """;
    String CHECKIN_RECORD_TXT = """
                                
                                📍 <b>境界：{level}</b>
                                🔹 <b>{userName} {camp}</b>
                                🧊 <b>战力：{power} (排名：{rankIndex})</b>
                                
                                ┌─详细属性
                                ┊
                                ┊ 🎮 竞技积分：{fraction} / {completeGame}场
                                ┊ 🤵 平民场次：{word_people_victory} / {word_people} ({people_percentage})
                                ┊ ⛄️ 卧底场次：{word_spy_victory} / {word_spy} ({spy_percentage})
                                ┊ 💎 流通货币：{dm} Dmail
                                ┊ 🎖 额外加成：无加成
                                ┊ 🕯 头衔：无头衔
                                └────────────
                                """;
    String CHECKIN_RECORD_CC_TXT = """
                                   
                                   ├─传承名单
                                   ┊
                                   ┊ 🍂 秋风萧瑟，您还没有传承弟子
                                   └────────────
                                   """;
    String INVITE_SINGLE = "{}(<b>当日累计: {}</b>)\t\t加入时间:{} \n";
    String CHECKIN_INVITE_SINGLE = "┊ {}(<b>当日累计: {}</b>)";
    String CHECKIN_INFO = """
                          
                          ├─✅签到成功｜今日总收益: {totalIv} <b>Dmail</b>
                          ┊
                          ┊  <b>{baseIv}</b><i>（随机基础）</i> {adminRed}
                          ┊  <b>{gameIv}</b><i>（游戏等级）</i> <b>{inheritIv}</b><i>（传承收益）</i>
                          └────────────
                          """;
    String RANK = "\n<b>▎无限远点的牵牛星 🏷 S{}赛季·积分榜 ♪</b>\n\n";
    String REAL_RANK = "\n<b>▎交叉坐标的星辰 🏷 S{}赛季·战力榜 ♪</b>\n\n";
    String TOP_TITLE = "\n<b>▎{} 🏷 S{}赛季·霸王榜 ♪</b>\n\n";
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
    String WattingTimeoutShutdown = "房主6min无动作，怀疑已经挂掉，游戏自动关闭！";
    String sendWord = """
                            你在<b>{}</b>群组中的游戏词语是: <b>{}</b>
                      """;
    String EXIT_GAME = "{} 花费3Dmail强行关闭了游戏";
    String EXIT_GAME2 = "{} 强行关闭了游戏，由于游戏异常，返回9Dmail";
    String EXIT_GAME_ERROR = "❌ 只有房主可以关闭游戏";
    String SPEECH_TIME = """
                         当前存活人: {}
                         接下来是发言时间，在进行描述的时候请加上<b>‘，’中文逗号前缀</b>，才算一次有效发言。
                         <b>{}</b>秒后将开始第<b>{}</b> 轮投票。\n
                         {}
                         """;
    String SPEAK_ORDER = "请由 <b>{}</b> 先进行首位发言；\n<b>{}</b> 进行第二位发言";
    String ViewWord = "查看词语";
    String GAME_START1 = "所有玩家准备就绪，游戏初始化中……";
    String GAME_START2 = "房主 {} 扣除10封Dmail，游戏初始化完成！";
    String GAME_START_ERROR = "游戏开启失败，{} 没有游戏开启权限！";
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
    String GAME_OVER_BOOM_SINGLE_PEOPLE = "<b>👨‍🌾平民 阵营达成【兄弟，不愧是你】成就，奖励积分+3！</b>\n";
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
    String USER_FULL = "✅ 参加人数达到{}人！全体(未逃跑)增加 <b>{}</b> 封Dmail \n";
    String RORATE_FULL = "✅ 激战回合数达到{}轮！全体(未逃跑)增加 <b>{}</b> 封Dmail \n";
    String USER_LEVEL_UP = "🚀 祝贺{} 飞升为 <b>【{}】</b>，收获礼包 <b>{}</b> 封Dmail！\n";
    // String USER_LEVEL_UP_FIRST = "🏆🚀🚀🚀 {} 成为<b>首个</b>飞升至 <b>【{}】</b>的超级大牛, 额外收获 <b>{}</b> 封Dmail！大家快来膜拜他！！！\n";
    String USER_LEVEL_UP_FIRST = """
                                 🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨
                                 🚨
                                 🚨  🏆 {}成为<b>【{}·之王】</b>
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
                         🚨  🏆 {}成为<b>【{}·之王】</b>
                         🚨  💰 收获<b>{}</b>封Dmail！{}
                         🚨  💓 <b>群内所有成员Dmail+50</b>
                         🚨  🎉 大家恭喜这个b！！！！！
                         🚨
                         🚨  <b>S{}赛季结束</b>，进入季后赛阶段！
                         🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨🚨
                         """;
    String USER_LEVEL_UP_HIGH = "注册码<b>{}</b>个";

    String ELIMINATED_IN_THIS_ROUND = "💀 第{}轮，本轮淘汰：";
    String GAME_SETTLEMENT = "游戏正在结算中，无法退出";
    String SURVIVAL_PERSONNEL = "\n剩余人员({}/{}):\n{}";
    String GENERATE_INVITE = "❤️您生成了一个传承邀请(1天)，请尽快发送给您的门人使用吧: {}\n";
    String RUN_AWAY_QUICKLY = "❗️{} 抢跑发言位，此次发言无效并扣除5封Dmail🫶\n";
    String WARNING_EDIT = "🚨游戏玩家: {} 修改了发言，请其余玩家注意甄别！\n";
    String NOT_VOTED_TIP = "❗️{} 两轮连续跑路，扣除5封Dmail🫶\n";
    String SPEAK_REPEAT = "❓️{} 无发言或重复发言，此次发言无效并扣除2封Dmail🫶\n";
    String SPEAK_NOWAY = "❓️{} 违禁爆词，扣除5封Dmail🫶\n";
    String SPEAK_NOWAY_BIG = "❗️❗️❗️{} 严重违禁爆词，受到天罚，扣除{}(10%)封Dmail🤡\n";

    String RIGISTER_CODE1 = "WorldLine-7-Register_1ahvc4W1FF";
    String RIGISTER_CODE2 = "WorldLine-7-Register_p2WD69Zkwj";
    String RIGISTER_CODE3 = "WorldLine-30-Register_gJWA2BVMhV";

    String BOOM_WAITING = """
                          🌌🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪
                          🌌
                          🌌嘘～～～～～～噤声！
                          🌌屠夫缓缓从屁眼中抽出了五米长的钢刀…………
                          🌌
                          🌌🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪🔪
                          """;
    String BOOM_FAIL = "☠️❌☠️❌☠️ 【{}】突然猝死暴毙倒下，围观群众虽不明但都躲得远远的，生怕被碰瓷（Dmail-2）\n";

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

    // 看图猜成语------------------
    String COMMON_WIN = "✅ {}回答正确！\n收获宝箱📦 {}封Dmail~\n";
    String KTCCY_TIP = "<b>🌸[成语猜猜看(1h)]🌸</b>\n\n回答时请加上<b>‘。’中文句号前缀</b>，才算一次有效回答。";
    String KTCFH_TIP = "<b>💄[道观我最强(1h)]💄</b>\n\n回答时请加上<b>‘。’中文句号前缀</b>，才算一次有效回答。";
    // 惊喜礼盒
    String SB_0401_TIP = "<b>🎉[愚人节专属礼盒(1h)]🎉</b>\n\n" +
            "祝大家节日快乐！当前点击花费50Dmail即可花费50Dmail，快来啊大爷们～";
    String SB_0401_GIFT = """
                          恭喜你在<b>🎉[愚人节专属礼盒]🎉</b>中获得奖品:
                            <b>{}</b> {}
                          
                          <i>ps: 建议可以不告诉别人中的什么骗下他们，让未参加的人也多多加入进来吧，嘿嘿</i>
                          """;
    String INVITE_LIST = """
                         
                         ◎ <b>{level}</b> {userName} 传承名单
                         
                         {list}
                         """;
    String INVITE_COLLECT = "🎉 你的传承弟子们昨日非常用功，你不禁抚须大笑，Dmail+<b>{}</b> \n";
    String INVITE_COLLECT2 = "🤔 你的传承弟子们都在闭关，宗门竟只你一人好像闲着，Dmail+<b>{}</b> \n";
    String INVITE_HELP = """
                         <b>传承系统</b>:
                         
                         1. 通过指令花费200Dmail可创建传承邀请链接，可以邀请其他用户加入本群，成为自己的传承弟子。
                         2. 每日可创建的传承数量有上限，上限与当前赛季游戏等级挂钩。
                         3. 传承弟子累计进群3周(21天)后出师，可以开启自己的传承邀请。
                         4. 在传承弟子未出师之前，每天游戏中获取的Dmail会按比例同时增发给自己的门主(需要门主手动领取)。
                         5. 门主可以通过指令获取弟子(未出师)名单，并且获取到传承弟子昨日游戏表现对应的Dmail奖励(每日一次)。
                         
                         /cc_inv 创建传承邀请（200Dmail）
                         /cc_help 召唤出当前帮助
                         """;
}