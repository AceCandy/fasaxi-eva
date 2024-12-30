package cn.acecandy.fasaxi.eva.sql.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.acecandy.fasaxi.eva.sql.entity.WodiWord;
import cn.acecandy.fasaxi.eva.sql.mapper.WodiWordMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 卧底词汇 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class WodiWordDao {

    @Resource
    private WodiWordMapper wodiWordMapper;

    /**
     * 随机获取词汇
     *
     * @return {@link WodiWord }
     */
    public WodiWord getRandom() {
        int totalRecords = wodiWordMapper.selectCount(null).intValue();
        if (totalRecords == 0) {
            return null;
        }

        int randomIndex = RandomUtil.randomInt(totalRecords);
        LambdaQueryWrapper<WodiWord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(WodiWord::getId).last("limit 3 offset " + randomIndex);

        List<WodiWord> wordList = wodiWordMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(wordList)) {
            return getRandom();
        }
        return wordList.stream()
                .min(Comparator.comparingInt(WodiWord::getPlayTime))
                .orElse(null);
    }

    /**
     * 随机获取词汇2
     *
     * @return {@link WodiWord }
     */
    public WodiWord getRandom2() {
        List<WodiWord> words = wodiWordMapper.selectRandomWord10();
        if (words.isEmpty()) {
            return null;
        }
        // 找到play_time最小的记录
        return words.stream().min(Comparator.comparingInt(WodiWord::getPlayTime)).orElse(null);
    }

    /**
     * 增加使用次数
     *
     * @param id 身份证件
     */
    public void upPlayTime(Integer id) {
        if (null == id) {
            return;
        }
        LambdaUpdateWrapper<WodiWord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WodiWord::getId, id);
        updateWrapper.setSql("play_time = play_time + 1");
        wodiWordMapper.update(null, updateWrapper);
    }
}