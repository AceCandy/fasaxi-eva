package cn.acecandy.fasaxi.eva.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.*;

/**
 * 游戏工具类(赛季子集)
 * <p>
 * 获取数据
 *
 * @author AceCandy
 * @since 2024/10/17
 */
@Slf4j
public class GameSubUtil {

    public static Integer levelUpScoreByLv1(Integer lv) {
        if (lv >= 6 && lv <= 9) {
            return 500;
        } else if (lv >= 10) {
            return 1000;
        }
        return lv * 100;
    }

    public static Integer levelUpScoreByLv2(Integer lv) {
        if (lv >= 10) {
            return 1000;
        }
        return 50 + lv * 50;
    }


    public static String levelByLv1(Integer lv) {
        return switch (lv) {
            case 0 -> "善良白丁";
            case 1 -> "钢铁暴民";
            case 2 -> "新生凤雏";
            case 3 -> "智计卧龙";
            case 4 -> "迷踪神探";
            case 5 -> "幻影刺客";
            case 6 -> "岐山王者";
            case 7 -> "九州霸王";
            case 8 -> "无天杀神";
            case 9 -> "乱世传奇";
            default -> lv >= 10 ? "地界至尊" : "地痞混子";
        };
    }

    public static String levelByLv2(Integer lv) {
        return switch (lv) {
            case 0 -> "普通路人";
            case 1 -> "古武高手";
            case 2 -> "仙路强者";
            case 3 -> "人界超赛";
            case 4 -> "地界超赛";
            case 5 -> "天界超赛";
            case 6 -> "原始超赛";
            case 7 -> "极道至尊";
            case 8 -> "羽化星空";
            case 9 -> "宇宙中枢";
            default -> lv >= 10 ? "圣破万维" : "未知生物";
        };
    }

    public static String levelByLv3(Integer lv) {
        return switch (lv) {
            case 0 -> "无用折纸";
            case 1 -> "环保塑料";
            case 2 -> "坚韧黑铁";
            case 3 -> "英勇黄铜";
            case 4 -> "不屈白银";
            case 5 -> "荣耀黄金";
            case 6 -> "华贵铂金";
            case 7 -> "璀璨钻石";
            case 8 -> "超凡大师";
            case 9 -> "傲世宗师";
            default -> lv >= 10 ? "最强王者" : "未知生物";
        };
    }

    public static Integer level1(Integer score) {
        if (score >= 0 && score < 100) {
            return 0;
        } else if (score >= 100 && score < 200) {
            return 1;
        } else if (score >= 200 && score < 300) {
            return 2;
        } else if (score >= 300 && score < 400) {
            return 3;
        } else if (score >= 400 && score < 500) {
            return 4;
        } else if (score >= 500 && score < 600) {
            return 5;
        } else if (score >= 600 && score < 800) {
            return 6;
        } else if (score >= 800 && score < 1000) {
            return 7;
        } else if (score >= 1000 && score < 1250) {
            return 8;
        } else if (score >= 1250 && score < 1888) {
            return 9;
        } else if (score >= 1888) {
            return 10;
        }
        return -1;
    }

    public static Integer level2(Integer score) {
        if (score >= 0 && score < 80) {
            return 0;
        } else if (score >= 80 && score < 180) {
            return 1;
        } else if (score >= 180 && score < 300) {
            return 2;
        } else if (score >= 300 && score < 440) {
            return 3;
        } else if (score >= 440 && score < 600) {
            return 4;
        } else if (score >= 600 && score < 780) {
            return 5;
        } else if (score >= 780 && score < 980) {
            return 6;
        } else if (score >= 980 && score < 1200) {
            return 7;
        } else if (score >= 1200 && score < 1521) {
            return 8;
        } else if (score >= 1521 && score < 1999) {
            return 9;
        } else if (score >= 1999) {
            return 10;
        }
        return -1;
    }

    /**
     * 获得首飞title
     *
     * @param season 季节
     * @return {@link String }
     */
    public static String getTopTitle(Integer season) {
        if (null == season) {
            return StrUtil.format(TOP_TITLE, SEASON1, 1);
        }
        return StrUtil.format(TOP_TITLE, getTopBySeason(season), season);
    }

    public static String getTopBySeason(Integer season) {
        if (season == 1) {
            return SEASON1;
        } else if (season == 2) {
            return SEASON2;
        } else if (season == 3) {
            return SEASON3;
        } else if (season == 4) {
            return SEASON4;
        } else if (season == 5) {
            return SEASON5;
        } else if (season == 6) {
            return SEASON6;
        } else if (season == 7) {
            return SEASON7;
        } else if (season == 8) {
            return SEASON8;
        } else if (season == 9) {
            return SEASON9;
        } else if (season == 10) {
            return SEASON10;
        }

        return SEASON1;
    }
}