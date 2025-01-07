package cn.acecandy.fasaxi.eva.dao.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.acecandy.fasaxi.eva.dao.entity.WodiUser;
import cn.acecandy.fasaxi.eva.dao.mapper.WodiUserMapper;
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
public class WodiUserDao {

    @Resource
    private WodiUserMapper wodiUserMapper;

    public List<WodiUser> findGtFraction(Integer fraction) {
        if (null == fraction) {
            return CollUtil.newArrayList();
        }
        LambdaQueryWrapper<WodiUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(WodiUser::getFraction, fraction);
        return wodiUserMapper.selectList(wrapper);
    }

    public List<WodiUser> findByTgId(List<Long> telegramIds) {
        if (CollUtil.isEmpty(telegramIds)) {
            return CollUtil.newArrayList();
        }
        LambdaQueryWrapper<WodiUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WodiUser::getTelegramId, telegramIds)
                .orderByAsc(WodiUser::getId)
        ;
        return wodiUserMapper.selectList(wrapper);
    }

    public WodiUser findByTgId(Long telegramId) {
        if (null == telegramId) {
            return null;
        }
        return CollUtil.getFirst(findByTgId(CollUtil.newArrayList(telegramId)));
    }

    public WodiUser findByGroupIdIfExist(Long telegramId) {
        WodiUser wodiUser = findByTgId(telegramId);
        if (wodiUser == null) {
            wodiUser = new WodiUser();
            wodiUser.setTelegramId(telegramId);
            insertOrUpdate(wodiUser);
            wodiUser = findByTgId(telegramId);
        }
        return wodiUser;
    }

    public List<WodiUser> selectRank() {
        LambdaQueryWrapper<WodiUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(WodiUser::getFraction, 0).orderByDesc(WodiUser::getFraction);
        return wodiUserMapper.selectList(queryWrapper);
    }

    /**
     * 插入或更新
     *
     * @param wodiUser 沃迪集团
     * @return int
     */
    public int insertOrUpdate(WodiUser wodiUser) {
        if (null == wodiUser.getId()) {
            return wodiUserMapper.insert(wodiUser);
        }
        return wodiUserMapper.updateById(wodiUser);
    }

    public void upFraction(Long telegramId, Integer fraction) {
        if (null == telegramId || null == fraction) {
            return;
        }
        LambdaUpdateWrapper<WodiUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WodiUser::getTelegramId, telegramId);
        updateWrapper.setSql("fraction = fraction + " + fraction);
        wodiUserMapper.update(null, updateWrapper);
    }

    public void upCompleteGame(List<Long> telegramIds) {
        if (CollUtil.isEmpty(telegramIds)) {
            return;
        }
        LambdaUpdateWrapper<WodiUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(WodiUser::getTelegramId, telegramIds);
        updateWrapper.setSql("complete_game = complete_game + 1");
        wodiUserMapper.update(null, updateWrapper);
    }

    public void upWordPeople(List<Long> telegramIds) {
        if (CollUtil.isEmpty(telegramIds)) {
            return;
        }
        LambdaUpdateWrapper<WodiUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(WodiUser::getTelegramId, telegramIds);
        updateWrapper.setSql("word_people = word_people + 1");
        wodiUserMapper.update(null, updateWrapper);
    }

    public void upWordSpy(List<Long> telegramIds) {
        if (CollUtil.isEmpty(telegramIds)) {
            return;
        }
        LambdaUpdateWrapper<WodiUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(WodiUser::getTelegramId, telegramIds);
        updateWrapper.setSql("word_spy = word_spy + 1");
        wodiUserMapper.update(null, updateWrapper);
    }

    public void upWordPeopleVictory(List<Long> telegramIds) {
        if (CollUtil.isEmpty(telegramIds)) {
            return;
        }
        LambdaUpdateWrapper<WodiUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(WodiUser::getTelegramId, telegramIds);
        updateWrapper.setSql("word_people_victory = word_people_victory + 1");
        wodiUserMapper.update(null, updateWrapper);
    }

    public void upWordSpyVictory(List<Long> telegramIds) {
        if (CollUtil.isEmpty(telegramIds)) {
            return;
        }
        LambdaUpdateWrapper<WodiUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(WodiUser::getTelegramId, telegramIds);
        updateWrapper.setSql("word_spy_victory = word_spy_victory + 1");
        wodiUserMapper.update(null, updateWrapper);
    }

    public void upJoinGame(List<Long> telegramIds) {
        if (CollUtil.isEmpty(telegramIds)) {
            return;
        }
        LambdaUpdateWrapper<WodiUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(WodiUser::getTelegramId, telegramIds);
        updateWrapper.setSql("join_game = join_game + 1");
        wodiUserMapper.update(null, updateWrapper);
    }

    public void upJoinGame(Long telegramId) {
        if (null == telegramId) {
            return;
        }
        upJoinGame(CollUtil.newArrayList(telegramId));
    }

    public void updateUserData(Long telegramId, String userName, String firstName, String lastName) {
        if (null == telegramId) {
            return;
        }
        WodiUser dbUser = findByGroupIdIfExist(telegramId);
        if (null == dbUser) {
            return;
        }
        if (!StrUtil.equals(dbUser.getUserName(), userName) ||
                !StrUtil.equals(dbUser.getFirstName(), firstName) ||
                !StrUtil.equals(dbUser.getLastName(), lastName)) {
            dbUser.setUserName(StrUtil.subPre(userName, 24));
            dbUser.setFirstName(StrUtil.subPre(firstName, 24));
            dbUser.setLastName(StrUtil.subPre(lastName, 24));
            insertOrUpdate(dbUser);
        }
    }
}