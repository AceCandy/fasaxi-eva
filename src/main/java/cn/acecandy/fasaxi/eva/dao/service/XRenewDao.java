package cn.acecandy.fasaxi.eva.dao.service;

import cn.acecandy.fasaxi.eva.dao.entity.XRenew;
import cn.acecandy.fasaxi.eva.dao.mapper.XRenewMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
     * @param iv    低压
     * @param codes 代码
     * @return boolean
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean insertXRenw(Integer iv, List<String> codes) {
        if (null == iv || CollUtil.isEmpty(codes)) {
            return false;
        }
        List<XRenew> xRenews = codes.stream().map(code -> {
            XRenew xInvite = new XRenew();
            xInvite.setIv(iv);
            xInvite.setCode(code);
            return xInvite;
        }).toList();

        return saveBatch(xRenews);
    }

    /**
     * 更新
     *
     * @param xRenew x续订
     */
    public boolean update(XRenew xRenew) {
        if (null == xRenew) {
            return false;
        }
        return saveOrUpdate(xRenew);
    }

    /**
     * 按code查找
     *
     * @param code 代码
     * @return {@link XRenew }
     */
    public XRenew findByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        LambdaQueryWrapper<XRenew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XRenew::getCode, code).eq(XRenew::getIsUse, false);
        ;

        return baseMapper.selectOne(wrapper);
    }

}