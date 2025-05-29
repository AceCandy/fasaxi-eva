package cn.acecandy.fasaxi.eva.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 赛季4-等级规则
 *
 * @author AceCandy
 * @since 2024/10/16
 */
@AllArgsConstructor
@Getter
public enum Season4Lv {
    /**
     * 1-10级别 炼气期→筑基期→结丹期→元婴期→化神期→炼虚期→合体期→大乘期→真仙境→金仙境→太乙境→大罗境→道祖境
     */
    LV_0(0, "人界", "凡人", Integer.MIN_VALUE, 68),
    LV_1(1, "人界", "练气期", 68, 168),
    LV_2(2, "人界", "筑基期", 168, 288),
    LV_3(3, "人界", "结丹期", 288, 438),
    LV_4(4, "人界", "元婴期", 438, 638),
    LV_5(5, "人界", "化神期", 638, 888),

    LV_6(6, "灵界", "练虚期", 888, 1188),
    LV_7(7, "灵界", "合体期", 1188, 1488),
    LV_8(8, "灵界", "大乘期", 1488, 1888),

    LV_9(9, "仙界", "真仙境", 1888, 2338),
    LV_10(10, "仙界", "金仙境", 2338, 2788),
    LV_11(11, "仙界", "太乙境", 2788, 3288),
    LV_12(12, "仙界", "大罗境", 3288, 4088),

    LV_13(13, "天道", "道祖境", 4088, Integer.MAX_VALUE),
    ;

    private final Integer lv;
    private final String realm;
    private final String title;
    private final Integer min;
    private final Integer max;

    public static Season4Lv scoreTo(int score) {
        for (Season4Lv sl : Season4Lv.values()) {
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
        Season4Lv sl = lvTo(lv);
        if (StrUtil.equals(sl.realm, "人界")) {
            return 100;
        } else if (StrUtil.equals(sl.realm, "灵界")) {
            return 250;
        } else if (StrUtil.equals(sl.realm, "仙界")) {
            return 400;
        } else if (StrUtil.equals(sl.realm, "天道")) {
            return 1000;
        }
        throw new RuntimeException("领域缺失");
    }

    public static Season4Lv lvTo(int lv) {
        for (Season4Lv sl : Season4Lv.values()) {
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