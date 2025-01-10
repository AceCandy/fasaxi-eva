package cn.acecandy.fasaxi.eva.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.extra.pinyin.PinyinUtil;
import lombok.extern.slf4j.Slf4j;

import java.text.Collator;
import java.util.List;
import java.util.Locale;

import static cn.hutool.core.text.CharSequenceUtil.EMPTY;
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
        return PinyinUtil.getPinyin(chinese).toUpperCase(Locale.ROOT);
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

    public static void main(String[] args) {
        List<String> sl = CollUtil.newArrayList("宝马", "包离", "报天", "宝俊", "包天田");
        sl.sort((o1, o2) -> Collator.getInstance(Locale.CHINA).compare(o1, o2));

        Console.log(getPingYin("重庆"));
        Console.log(getPingYin("蔚来"));
        Console.log(getPingYin("007车"));
        Console.log(getFirstLetter("重庆"));
        Console.log(getFirstLetters("重庆"));
        Console.log(getFirstLetters("007车"));
        Console.log(PinyinUtil.getPinyin("重庆"));
        Console.log(PinyinUtil.getFirstLetter("重庆", EMPTY));
        Console.log(PinyinUtil.getFirstLetter("重庆", EMPTY));
        List<String> speakList = CollUtil.newArrayList();
        speakList.add("");
        speakList.add("");
        Console.log(speakList);
        if (CollUtil.contains(speakList,"")) {
            Console.log("contains");
        }
    }
}