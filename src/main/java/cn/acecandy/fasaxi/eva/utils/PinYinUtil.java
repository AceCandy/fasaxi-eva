package cn.acecandy.fasaxi.eva.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static cn.hutool.core.text.CharSequenceUtil.EMPTY;
import static cn.hutool.core.text.CharSequenceUtil.SPACE;
import static cn.hutool.core.text.CharSequenceUtil.isBlank;
import static cn.hutool.core.text.CharSequenceUtil.subPre;
import static cn.hutool.core.text.CharSequenceUtil.upperFirst;

/**
 * 拼音工具类
 *
 * @author tangningzhu
 * @since 2023/12/20
 */
@Slf4j
public final class PinYinUtil extends PinyinUtil {

    private PinYinUtil() {
    }

    /**
     * 将字符串中的中文转化为拼音,其他字符不变(全大写输出)
     *
     * @param chinese 中文字符串
     * @return {@link String}
     */
    public static String getPingYin(String chinese) {
        if (isBlank(chinese)) {
            return EMPTY;
        }
        return PinyinUtil.getPinyin(chinese, EMPTY).toUpperCase(Locale.ROOT);
    }

    public static List<String> getPingYinList(String chinese) {
        if (isBlank(chinese)) {
            return CollUtil.newArrayList();
        }
        return StrUtil.splitTrim(PinyinUtil.getPinyin(chinese), SPACE);
    }

    /**
     * 获取中文首字母对应的数值
     * <p>
     * 不区分大小写
     *
     * @param chinese 中文
     * @return int
     */
    public static int getFirstInt(String chinese) {
        if (isBlank(chinese)) {
            return 0;
        }
        return getFirstLetter(chinese).charAt(0);
    }


    /**
     * 获取字符串中第一个字符首字母,若第一个字符不是中文则返回第一个字符
     * <p>
     * 不区分大小写 返回大写
     *
     * @param chinese 中文
     * @return String
     */
    public static String getFirstLetter(String chinese) {
        if (isBlank(chinese)) {
            return EMPTY;
        }
        String letterStr = PinyinUtil.getFirstLetter(chinese, EMPTY);
        return upperFirst(subPre(letterStr, 1));
    }

    /**
     * 获取拼音首字母 比如重庆->CQ
     *
     * @param chinese 中国人
     * @return {@link String }
     */
    public static String getFirstLetters(String chinese) {
        if (isBlank(chinese)) {
            return EMPTY;
        }
        String letterStr = PinyinUtil.getFirstLetter(chinese, EMPTY);
        return letterStr.toUpperCase();
    }

    /**
     * 查找字符在所有文字中存在大于2
     *
     * @param text 当前文本
     * @param have 查找词
     * @return boolean
     */
    public static boolean findTwoChar(String text, String have) {
        if (isBlank(text)) {
            return false;
        }
        Set<String> set = CollUtil.newHashSet();
        for (char c : have.toCharArray()) {
            if (StrUtil.contains(text, c)) {
                set.add(String.valueOf(c));
            }
        }
        return set.size() >= 2;
    }

    /**
     * 查找字符在所有文字中存在
     *
     * @param text 文本
     * @param have 有
     * @return boolean
     */
    public static boolean findAllChar(String text, String have) {
        if (isBlank(text)) {
            return false;
        }
        for (char c : have.toCharArray()) {
            if (!StrUtil.contains(text, c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 查找文本中是否包含 词A和词B除开相同部分外的剩余内容
     *
     * @param text  文本
     * @param wordA 单词
     * @param wordB Wordb
     * @return boolean
     */
    public static boolean findRemainingInText(String text, String wordA, String wordB) {
        if (StrUtil.isBlank(text) || StrUtil.isBlank(wordA) || StrUtil.isBlank(wordB)) {
            return false;
        }

        // 计算共同前缀长度
        int prefixLen = 0;
        int minLen = Math.min(wordA.length(), wordB.length());
        while (prefixLen < minLen && wordA.charAt(prefixLen) == wordB.charAt(prefixLen)) {
            prefixLen++;
        }

        // 计算共同后缀长度
        int suffixLenA = wordA.length() - prefixLen;
        int suffixLenB = wordB.length() - prefixLen;
        int suffixLen = 0;
        while (suffixLen < Math.min(suffixLenA, suffixLenB) &&
                wordA.charAt(wordA.length() - 1 - suffixLen) == wordB.charAt(wordB.length() - 1 - suffixLen)) {
            suffixLen++;
        }

        // 提取剩余部分
        String remainingA = prefixLen + suffixLen < wordA.length() ?
                wordA.substring(prefixLen, wordA.length() - suffixLen) : "";
        String remainingB = prefixLen + suffixLen < wordB.length() ?
                wordB.substring(prefixLen, wordB.length() - suffixLen) : "";

        // 检查剩余部分是否存在于text中
        return (StrUtil.isNotBlank(remainingA) && StrUtil.containsIgnoreCase(text, remainingA)) ||
                (StrUtil.isNotBlank(remainingB) && StrUtil.containsIgnoreCase(text, remainingB));
    }

    public static void main(String[] args) {
        // Console.log(findAllChar("奇奇怪怪的曲", "曲奇1"));
        // Console.log(PinYinUtil.findAllChar("白色板子", "白板"));
        // Console.log(PinYinUtil.findTwoChar("一堆垃圾", "堆堆袜"));
        // Console.log(PinYinUtil.findTwoChar("白色板子", "白"));
        // Console.log(PinYinUtil.findTwoChar("奇奇怪怪的曲", "曲奇1"));
        Console.log(PinYinUtil.findRemainingInText("一排对手", "足球","排球"));
        Console.log(PinYinUtil.findRemainingInText("一包大眼睛", "双肩包","单肩包"));
    }
}