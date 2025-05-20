package cn.acecandy.fasaxi.eva.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * fh游戏工具类
 * <p>
 * 获取数据
 *
 * @author AceCandy
 * @since 2024/10/17
 */
@Slf4j
public final class FhUtil {
    private FhUtil() {
    }

    /**
     * 搜索海报
     *
     * @param root 根
     * @return {@link Path }
     */
    @SneakyThrows
    public static Path searchPoster(String root) {
        final AtomicInteger counter = new AtomicInteger(0);
        final Path[] result = new Path[1];
        try (Stream<Path> stream = Files.find(Paths.get(root), Integer.MAX_VALUE,
                (p, a) -> a.isRegularFile() && p.endsWith("poster.jpg")).parallel()) {
            stream.forEach(file -> reservoirSampling(file, counter, result));
        }
        return counter.get() == 0 ? null : result[0];
    }

    private static final Lock LOCK = new ReentrantLock();

    private static void reservoirSampling(Path file, AtomicInteger counter, Path[] result) {
        int count = counter.getAndIncrement();
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // 无锁优化：使用CAS减少竞争
        if (count == 0) {
            LOCK.lock();
            try {
                result[0] = file;
            } finally {
                LOCK.unlock();
            }
        } else if (rand.nextInt(count + 1) == 0) {
            LOCK.lock();
            try {
                result[0] = file;
            } finally {
                LOCK.unlock();
            }
        }
    }

    /**
     * 番号 匹配正则
     */
    private static final Pattern FH_PATTERN =
            Pattern.compile("^([A-Z0-9]+[-_][A-Z0-9\\d]{2,})(?:[-_][A-Z]+)?\\b.*");

    /**
     * 提取标准番号
     *
     * @param original 原来
     * @return {@link String }
     */
    public static String standardFhName(String original) {
        if (StrUtil.isBlank(original)) {
            return original;
        }
        return CollUtil.getFirst(ReUtil.findAll(FH_PATTERN, StrUtil.trim(original), 1));
    }

    /**
     * 获取番号名称
     *
     * @param filePath 文件路径
     * @return {@link String }
     */
    public static String getFhName(String filePath) {
        int end = StrUtil.lastIndexOfIgnoreCase(filePath, "/");
        if (end == -1) {
            // Windows兼容
            end = StrUtil.lastIndexOfIgnoreCase(filePath, "\\");
        }

        if (end > 0) {
            int start = StrUtil.lastIndexOfIgnoreCase(filePath, "/", end - 1);
            if (start == -1) {
                start = StrUtil.lastIndexOfIgnoreCase(filePath, "\\", end - 1);
            }
            String fhName = StrUtil.sub(filePath, (start != -1) ? start + 1 : 0, end);
            return standardFhName(fhName);
        }
        return null;
    }

    public static void main(String[] args) {
        String filePath = "/private/tmp/test/104DANDY/104DANDY-818-C 座ったままの男を一切动かさないS字尻振り骑乘位で骨抜きにする美尻キャビンアテンダント/poster.jpg";
        Console.log(getFhName(filePath));
        Console.log(StrUtil.lastIndexOfIgnoreCase(filePath, "/vvvv"));
        Console.log(filePath.lastIndexOf('/'));
    }
}