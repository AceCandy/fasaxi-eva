package cn.acecandy.fasaxi.eva.dao.service;

import cn.acecandy.fasaxi.eva.dao.entity.XInvite;
import cn.acecandy.fasaxi.eva.dao.entity.XRenew;
import cn.acecandy.fasaxi.eva.dao.mapper.XInviteMapper;
import cn.acecandy.fasaxi.eva.dao.mapper.XRenewMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 邀请 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class XRenewDao extends ServiceImpl<XRenewMapper, XRenew> {

    /**
     * 创建
     *
     * @param tgId tg id
     * @param url  网址
     * @return boolean
     */
    public boolean insertInviter(Long tgId, String url) {
        if (null == tgId || StrUtil.isBlank(url)) {
            return false;
        }
        XInvite xInvite = new XInvite();
        xInvite.setUrl(url);
        xInvite.setInviterId(tgId);
        return xInviteMapper.insert(xInvite) > 0;
    }

    /**
     * 更新受邀者
     *
     * @param xInvite 实体
     */
    public void updateInvitee(XInvite xInvite) {
        if (null == xInvite) {
            return;
        }
        xInviteMapper.insertOrUpdate(xInvite);
    }

    /**
     * 更新受邀者
     *
     * @param tgIds 列表
     */
    public void updateCollectTime(List<Long> tgIds, Date collectTime) {
        if (CollUtil.isEmpty(tgIds)) {
            return;
        }
        LambdaUpdateWrapper<XInvite> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(XInvite::getInviteeId, tgIds);
        updateWrapper.set(XInvite::getCollectTime, collectTime);
        xInviteMapper.update(null, updateWrapper);
    }

    /**
     * 按url查找
     *
     * @param url 网址
     * @return {@link XInvite }
     */
    public XInvite findByUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        LambdaQueryWrapper<XInvite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XInvite::getUrl, url)
        ;
        return xInviteMapper.selectOne(wrapper);
    }

    /**
     * 按用户查找最后一次邀请
     *
     * @param tgId 网址
     * @return {@link XInvite }
     */
    public XInvite findByInviterLast(Long tgId) {
        if (null == tgId) {
            return null;
        }
        LambdaQueryWrapper<XInvite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XInvite::getInviterId, tgId)
                .orderByDesc(XInvite::getCreateTime).last("limit 1");
        ;
        return xInviteMapper.selectOne(wrapper);
    }

    /**
     * 查询门人名单
     *
     * @param tgId 网址
     * @return {@link XInvite }
     */
    public List<XInvite> findInviteeByInviter(Long tgId) {
        if (null == tgId) {
            return CollUtil.newArrayList();
        }
        LambdaQueryWrapper<XInvite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XInvite::getInviterId, tgId)
                .isNotNull(XInvite::getInviteeId)
                .orderByAsc(XInvite::getCreateTime);
        ;
        return xInviteMapper.selectList(wrapper);
    }

    /**
     * 查询师尊
     *
     * @param tgId 网址
     * @return {@link XInvite }
     */
    public XInvite findByInvitee(Long tgId) {
        if (null == tgId) {
            return null;
        }
        LambdaQueryWrapper<XInvite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XInvite::getInviteeId, tgId)
                .orderByDesc(XInvite::getCreateTime);
        ;
        return CollUtil.getFirst(xInviteMapper.selectList(wrapper));
    }

    /**
     * 按用户最近一天创建的数量
     *
     * @param tgId 网址
     * @return {@link XInvite }
     */
    public Long cntByInviterToday(Long tgId) {
        if (null == tgId) {
            return 0L;
        }
        LambdaQueryWrapper<XInvite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XInvite::getInviterId, tgId)
                .ge(XInvite::getCreateTime, System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        ;
        return xInviteMapper.selectCount(wrapper);
    }
}