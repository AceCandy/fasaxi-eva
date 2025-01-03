package cn.acecandy.fasaxi.eva.sql.service;

import cn.acecandy.fasaxi.eva.sql.entity.Emby;
import cn.acecandy.fasaxi.eva.sql.mapper.EmbyMapper;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 卧底用户 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class EmbyDao {

    @Resource
    private EmbyMapper embyMapper;

    public void upIv(Long telegramId, Integer iv) {
        if (null == telegramId || null == iv) {
            return;
        }
        LambdaUpdateWrapper<Emby> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Emby::getTg, telegramId);
        updateWrapper.setSql("iv = iv + " + iv);
        embyMapper.update(null, updateWrapper);
    }

    public void allUpIv(Integer iv) {
        if (null == iv) {
            return;
        }
        LambdaUpdateWrapper<Emby> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.gt(Emby::getIv, 0);
        updateWrapper.setSql("iv = iv + " + iv);
        embyMapper.update(null, updateWrapper);
    }

    public List<Emby> findByTgId(List<Long> telegramIds) {
        if (CollUtil.isEmpty(telegramIds)) {
            return CollUtil.newArrayList();
        }
        LambdaQueryWrapper<Emby> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Emby::getTg, telegramIds)
        ;
        return embyMapper.selectList(wrapper);
    }

    public Emby findByTgId(Long telegramId) {
        if (null == telegramId) {
            return null;
        }
        return CollUtil.getFirst(findByTgId(CollUtil.newArrayList(telegramId)));
    }
}