package cn.acecandy.fasaxi.eva.sql.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.acecandy.fasaxi.eva.sql.entity.Emby;
import cn.acecandy.fasaxi.eva.sql.mapper.EmbyMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
}