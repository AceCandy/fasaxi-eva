package cn.acecandy.fasaxi.eva.dao.service;

import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.mapper.EmbyMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
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

    public void init(Long telegramId, Integer iv) {
        if (null == telegramId) {
            return;
        }
        Emby emby = Emby.builder().tg(telegramId).iv(iv).build();
        embyMapper.insertOrUpdate(emby);
    }

    public void destory(Long telegramId) {
        if (null == telegramId) {
            return;
        }
        Emby emby = Emby.builder().tg(telegramId).lv("e").build();
        embyMapper.insertOrUpdate(emby);
    }

    public void checkIn(Long telegramId) {
        if (null == telegramId) {
            return;
        }
        ThreadUtil.execAsync(() -> {
            Emby emby = Emby.builder().tg(telegramId).ch(DateUtil.date()).build();
            embyMapper.insertOrUpdate(emby);
        });
    }

    public void upIv(Long telegramId, Integer iv) {
        if (null == telegramId || null == iv || 0 == iv) {
            return;
        }
        ThreadUtil.execAsync(() -> {
            LambdaUpdateWrapper<Emby> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Emby::getTg, telegramId);
            updateWrapper.setSql("iv = iv + " + iv);
            embyMapper.update(null, updateWrapper);
        });
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

    /**
     * 查找有号账户
     *
     * @return {@link List }<{@link Emby }>
     */
    public List<Emby> findOnHasAccount() {
        LambdaQueryWrapper<Emby> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Emby::getLv, CollUtil.newArrayList("a", "b", "c"))
        ;
        return embyMapper.selectList(wrapper);
    }
}