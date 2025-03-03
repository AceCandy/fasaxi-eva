package cn.acecandy.fasaxi.eva.dao.service;

import cn.acecandy.fasaxi.eva.dao.entity.GameKtccy;
import cn.acecandy.fasaxi.eva.dao.entity.WodiWord;
import cn.acecandy.fasaxi.eva.dao.mapper.GameKtccyMapper;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 看图猜成语 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class GameKtccyDao {

    @Resource
    private GameKtccyMapper gameKtccyMapper;

    /**
     * 随机获取词汇2
     *
     * @return {@link WodiWord }
     */
    public GameKtccy getRandom2() {
        List<GameKtccy> ktccy = gameKtccyMapper.selectRandomWord10();
        if (CollUtil.isEmpty(ktccy)) {
            return null;
        }
        // 找到play_time最小的记录
        return ktccy.stream().min(Comparator.comparingInt(GameKtccy::getPlayTime)).orElse(null);
    }

    /**
     * 增加使用次数
     *
     * @param id 身份证件
     */
    public void upPlayTime(Long id) {
        if (null == id) {
            return;
        }
        LambdaUpdateWrapper<GameKtccy> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GameKtccy::getId, id);
        updateWrapper.setSql("play_time = play_time + 1");
        gameKtccyMapper.update(null, updateWrapper);
    }
}