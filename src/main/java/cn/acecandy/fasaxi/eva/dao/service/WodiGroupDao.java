package cn.acecandy.fasaxi.eva.dao.service;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.acecandy.fasaxi.eva.dao.entity.WodiGroup;
import cn.acecandy.fasaxi.eva.dao.mapper.WodiGroupMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 卧底群组 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class WodiGroupDao {

    @Resource
    private WodiGroupMapper wodiGroupMapper;

    public List<WodiGroup> findByGroupId(List<Long> groupIds) {
        if (CollUtil.isEmpty(groupIds)) {
            return CollUtil.newArrayList();
        }
        LambdaQueryWrapper<WodiGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WodiGroup::getGroupId, groupIds)
                .orderByAsc(WodiGroup::getId)
        ;
        return wodiGroupMapper.selectList(wrapper);
    }

    public WodiGroup findByGroupId(Long groupId) {
        if (null == groupId) {
            return null;
        }
        return CollUtil.getFirst(findByGroupId(CollUtil.newArrayList(groupId)));
    }

    public WodiGroup findByGroupIdIfExist(Long groupId) {
        WodiGroup wodiGroup = findByGroupId(groupId);
        if (wodiGroup == null) {
            wodiGroup = new WodiGroup();
            wodiGroup.setGroupId(groupId);
            insertOrUpdate(wodiGroup);
            wodiGroup = findByGroupId(groupId);
        }
        return wodiGroup;
    }

    /**
     * 插入或更新
     *
     * @param wodiGroup 沃迪集团
     * @return int
     */
    public int insertOrUpdate(WodiGroup wodiGroup) {
        if (null == wodiGroup.getId()) {
            return wodiGroupMapper.insert(wodiGroup);
        }
        return wodiGroupMapper.updateById(wodiGroup);
    }

    public void upFinishGame(Long groupId) {
        if (null == groupId) {
            return;
        }
        LambdaUpdateWrapper<WodiGroup> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WodiGroup::getGroupId, groupId);
        updateWrapper.setSql("finish_game = finish_game + 1");
        wodiGroupMapper.update(null, updateWrapper);
    }

    public void upMaxOfPeople(Long groupId, Integer people) {
        if (null == groupId || null == people) {
            return;
        }
        LambdaUpdateWrapper<WodiGroup> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WodiGroup::getGroupId, groupId).lt(WodiGroup::getMaxOfPeople, people);
        updateWrapper.set(WodiGroup::getMaxOfPeople, people);
        wodiGroupMapper.update(null, updateWrapper);
    }

    public void updateGroupData(Long groupId, String userName, String title) {
        if (null == groupId) {
            return;
        }
        WodiGroup group = findByGroupIdIfExist(groupId);
        if (null == group) {
            return;
        }
        if (!StrUtil.equals(group.getUserName(), userName) || !StrUtil.equals(group.getTitle(), title)) {
            group.setUserName(StrUtil.subPre(userName, 24));
            group.setTitle(StrUtil.subPre(title, 24));
            group.setJoinTime(new Date());
            insertOrUpdate(group);
        }
    }
}