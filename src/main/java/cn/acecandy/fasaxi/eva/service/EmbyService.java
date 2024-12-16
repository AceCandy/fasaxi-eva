package cn.acecandy.fasaxi.eva.service;

import cn.hutool.core.util.StrUtil;
import cn.acecandy.fasaxi.eva.bean.req.VideoRedirectReq;
import cn.acecandy.fasaxi.eva.bean.vo.HeadVO;
import cn.acecandy.fasaxi.eva.bot.impl.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.utils.CacheUtil;
import cn.acecandy.fasaxi.eva.utils.EmbyUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Emby服务
 *
 * @author AceCandy
 * @since 2024/11/19
 */
@Slf4j
@Component
public class EmbyService {

    @Resource
    private EmbyTelegramBot tgBot;

    public String videoRedirect(HeadVO headVO, VideoRedirectReq req) {
        String ua = headVO.getUa();
        String mediaSourceId = req.getMediaSourceId();

        String redirectUrl = CacheUtil.getMediaKey(ua, mediaSourceId);
        if (StrUtil.isNotBlank(redirectUrl)) {
            log.info("[重定向] 获取缓存成功！{}:{} => {}", mediaSourceId, ua, redirectUrl);
            return redirectUrl;
        }
        redirectUrl = EmbyUtil.getItemInfo(ua, mediaSourceId);
        if (StrUtil.isNotBlank(redirectUrl)) {
            CacheUtil.setMediaKey(ua, mediaSourceId, redirectUrl);
            return redirectUrl;
        }
        return "";
    }
}