package cn.acecandy.fasaxi.eva.dao.service;

import cn.acecandy.fasaxi.eva.dao.entity.WodiTop;
import cn.acecandy.fasaxi.eva.dao.mapper.WodiTopMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.CURRENT_SEASON;

/**
 * 卧底用户 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class WodiTopDao {

    @Resource
    private WodiTopMapper wodiTopMapper;

    public List<WodiTop> selectTop() {
        return selectTop(CURRENT_SEASON);
    }

    public List<WodiTop> selectTop(Integer season) {
        LambdaQueryWrapper<WodiTop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WodiTop::getSeason, season).orderByAsc(WodiTop::getLevel);
        return wodiTopMapper.selectList(queryWrapper);
    }

    /**
     * 插入或更新
     *
     * @param wodiTop 沃迪集团
     * @return int
     */
    public int insertOrUpdate(WodiTop wodiTop) {
        if (null == wodiTop.getId()) {
            return wodiTopMapper.insert(wodiTop);
        }
        return wodiTopMapper.updateById(wodiTop);
    }
}