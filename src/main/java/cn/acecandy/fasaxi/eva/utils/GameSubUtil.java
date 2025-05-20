package cn.acecandy.fasaxi.eva.utils;

import cn.acecandy.fasaxi.eva.common.constants.GameTextConstants;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.CURRENT_SEASON;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.SEASON1;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TOP_TITLE;

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

    /**
     * 统一调用入口
     *
     * @param season 赛季编号（1=Season1Lv, 2=Season2Lv...）
     * @param params 方法参数（需与方法签名匹配）
     */
    @SneakyThrows
    public static Object invoke(Integer season, Object... params) {
        if (null == season) {
            season = CURRENT_SEASON;
        }
        String methodName = StackWalker.getInstance().walk(s -> s.skip(1)
                .findFirst().map(StackWalker.StackFrame::getMethodName).orElse("Unknown"));
        // 获取赛季对应的枚举类名
        String className = "cn.acecandy.fasaxi.eva.common.enums.Season" + season + "Lv";
        Method method = ReflectUtil.getMethod(Class.forName(className), methodName, ClassUtil.getClasses(params));
        return ReflectUtil.invokeStatic(method, params);
    }

    public static Integer scoreToLv(Integer score) {
        return (int) invoke(CURRENT_SEASON, score);
    }

    public static String lvToTitle(Integer lv) {
        return lvToTitle(lv, CURRENT_SEASON);
    }

    public static String lvToTitle(Integer lv, Integer season) {
        return (String) invoke(season, lv);
    }

    public static String scoreToTitle(Integer score) {
        return (String) invoke(CURRENT_SEASON, score);
    }

    public static Integer scoreToFirstUpGift(Integer score) {
        return (Integer) invoke(CURRENT_SEASON, score);
    }

    public static Integer lvToFirstUpGift(Integer lv) {
        return (Integer) invoke(CURRENT_SEASON, lv);
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

    @SneakyThrows
    public static String getTopBySeason(Integer season) {
        if (null == season) {
            season = CURRENT_SEASON;
        }
        return (String) ReflectUtil.getFieldValue(GameTextConstants.class, "SEASON" + season);
    }

    public static void main(String[] args) {
        Console.log(getTopBySeason(3));
    }
}