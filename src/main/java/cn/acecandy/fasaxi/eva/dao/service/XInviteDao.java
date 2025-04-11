package cn.acecandy.fasaxi.eva.dao.service;

import cn.acecandy.fasaxi.eva.dao.entity.XInvite;
import cn.acecandy.fasaxi.eva.dao.mapper.XInviteMapper;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邀请 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class XInviteDao {

    @Resource
    private XInviteMapper xInviteMapper;

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
}