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
public enum Season2Lv {
    /**
     * 1-10级别
     */
    LV_0(0, "普通路人", Integer.MIN_VALUE, 80),
    LV_1(1, "古武高手", 80, 180),
    LV_2(2, "仙路强者", 180, 320),
    LV_3(3, "人界超赛", 320, 500),
    LV_4(4, "地界超赛", 500, 700),
    LV_5(5, "天界超赛", 700, 950),
    LV_6(6, "原始超赛", 950, 1250),
    LV_7(7, "极道至尊", 1250, 1650),
    LV_8(8, "羽化星空", 1650, 2150),
    LV_9(9, "宇宙中枢", 2150, 2816),
    LV_10(10, "圣破万维", 2816, Integer.MAX_VALUE);

    private final Integer lv;
    private final String title;
    private final Integer min;
    private final Integer max;

    public static Season2Lv scoreTo(int score) {
        for (Season2Lv sl : Season2Lv.values()) {
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

    public static Season2Lv lvTo(int lv) {
        for (Season2Lv sl : Season2Lv.values()) {
            if (sl.lv.equals(lv)) {
                return sl;
            }
        }
        throw new RuntimeException("等级缺失");
    }

    public static String lvToTitle(int lv) {
        return lvTo(lv).title;
    }
}