package cn.acecandy.fasaxi.eva.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 赛季3-等级规则
 *
 * @author AceCandy
 * @since 2024/10/16
 */
@AllArgsConstructor
@Getter
public enum Season3Lv {
    /**
     * 1-10级别
     */
    LV_0(0, "无用折纸", Integer.MIN_VALUE, 100),
    LV_1(1, "环保塑料", 100, 200),
    LV_2(2, "坚韧黑铁", 200, 300),
    LV_3(3, "英勇黄铜", 300, 500),
    LV_4(4, "不屈白银", 500, 750),
    LV_5(5, "荣耀黄金", 750, 1050),
    LV_6(6, "华贵铂金", 1050, 1450),
    LV_7(7, "璀璨钻石", 1450, 1950),
    LV_8(8, "超凡大师", 1950, 2616),
    LV_9(9, "傲世宗师", 2616, 3504),
    LV_10(10, "最强王者", 3504, Integer.MAX_VALUE);

    private final Integer lv;
    private final String title;
    private final Integer min;
    private final Integer max;

    public static Season3Lv scoreTo(int score) {
        for (Season3Lv sl : Season3Lv.values()) {
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
        if (lv >= LV_10.lv) {
            return 1000;
        }
        return 50 + lv * 50;
    }

    public static Season3Lv lvTo(int lv) {
        for (Season3Lv sl : Season3Lv.values()) {
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