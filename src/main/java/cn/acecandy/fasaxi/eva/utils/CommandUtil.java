package cn.acecandy.fasaxi.eva.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 命令 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
@Slf4j
public final class CommandUtil {
    private CommandUtil() {
    }

    /**
     * 是红包相关命令
     *
     * @param command 命令
     * @return boolean
     */
    public static boolean isRedCommand(String command) {
        return StrUtil.startWith(command, "/red");
    }

    /**
     * 是警告相关命令
     *
     * @param command 命令
     * @return boolean
     */
    public static boolean isWarnCommand(String command) {
        return StrUtil.startWith(command, "/warn");
    }

    /**
     * 是wd相关命令
     *
     * @param command 命令
     * @return boolean
     */
    public static boolean isWdCommand(String command) {
        return StrUtil.startWith(command, "/wd");
    }

    /**
     * 是Cc相关命令
     *
     * @param command 命令
     * @return boolean
     */
    public static boolean isCcCommand(String command) {
        return StrUtil.startWith(command, "/cc");
    }

    /**
     * 是Xm相关命令
     *
     * @param command 命令
     * @return boolean
     */
    public static boolean isXmCommand(String command) {
        return StrUtil.startWith(command, "/xm");
    }
}