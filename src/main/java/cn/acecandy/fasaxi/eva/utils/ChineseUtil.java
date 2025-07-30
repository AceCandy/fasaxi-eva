package cn.acecandy.fasaxi.eva.utils;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import lombok.extern.slf4j.Slf4j;

import static cn.hutool.core.text.CharSequenceUtil.EMPTY;
import static cn.hutool.core.text.CharSequenceUtil.isBlank;

/**
 * 拼音工具类
 *
 * @author tangningzhu
 * @since 2023/12/20
 */
@Slf4j
public final class ChineseUtil {

    private ChineseUtil() {
    }

    /**
     * 将字符串中的中文转化为拼音,其他字符不变(全大写输出)
     *
     * @param chinese 中文字符串
     * @return {@link String}
     */
    public static String toSimple(String chinese) {
        if (isBlank(chinese)) {
            return EMPTY;
        }
        return ZhConverterUtil.toSimple(chinese);
    }
}