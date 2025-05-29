package cn.acecandy.fasaxi.eva.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 赛季2-等级规则
 *
 * @author AceCandy
 * @since 2024/10/16
 */
@AllArgsConstructor
@Getter
public enum Season1Lv {
    /**
     * 1-10级别
     */
    LV_0(0, "善良白丁", Integer.MIN_VALUE, 100),
    LV_1(1, "钢铁暴民", 100, 200),
    LV_2(2, "新生凤雏", 200, 300),
    LV_3(3, "智计卧龙", 300, 400),
    LV_4(4, "迷踪神探", 400, 500),
    LV_5(5, "幻影刺客", 500, 600),
    LV_6(6, "岐山王者", 600, 800),
    LV_7(7, "九州霸王", 800, 1000),
    LV_8(8, "无天杀神", 1000, 1250),
    LV_9(9, "乱世传奇", 1250, 1888),
    LV_10(10, "地界至尊", 1888, Integer.MAX_VALUE);

    private final Integer lv;
    private final String title;
    private final Integer min;
    private final Integer max;

    public static Season1Lv scoreTo(int score) {
        for (Season1Lv sl : Season1Lv.values()) {
            if (score >= sl.min && score < sl.max) {
                return sl;
            }
        }
        throw new RuntimeException("等级缺失");
    }

    /**
     * 分数 -> 等级
     *
     * @param score 得分
     * @return {@link Integer }
     */
    public static Integer scoreToLv(int score) {
        return scoreTo(score).lv;
    }

    /**
     * 分数 -> 头衔
     *
     * @param score 得分
     * @return {@link Integer }
     */
    public static String scoreToTitle(int score) {
        return scoreTo(score).title;
    }

    public static Integer scoreToFirstUpGift(int score) {
        return lvToFirstUpGift(scoreToLv(score));
    }

    /**
     * 首飞礼包礼物
     *
     * @param lv 低压
     * @return {@link Integer }
     */
    public static Integer lvToFirstUpGift(int lv) {
        if (lv >= LV_6.lv && lv <= LV_9.lv) {
            return 500;
        } else if (lv >= LV_10.lv) {
            return 1000;
        }
        return lv * 100;
    }

    public static Season1Lv lvTo(int lv) {
        for (Season1Lv sl : Season1Lv.values()) {
            if (sl.lv.equals(lv)) {
                return sl;
            }
        }
        throw new RuntimeException("等级缺失");
    }

    public static Integer lvToMin(int lv) {
        return lvTo(lv).min;
    }

    public static String lvToTitle(int lv) {
        return lvTo(lv).title;
    }
}