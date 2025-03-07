package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.bot.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.bot.game.Game;
import cn.acecandy.fasaxi.eva.dao.entity.GameKtccy;
import cn.acecandy.fasaxi.eva.dao.service.GameKtccyDao;
import cn.acecandy.fasaxi.eva.utils.CommonGameUtil;
import cn.acecandy.fasaxi.eva.utils.GameListUtil;
import cn.acecandy.fasaxi.eva.utils.GameUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.KTCCY_TIP;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.KTCFH_TIP;

/**
 * 通用定时任务 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class CommonGameService {

    @Resource
    private EmbyTelegramBot tgBot;

    @Resource
    private GameKtccyDao gameKtccyDao;

    @Value("${game.fh-path}")
    private String fhPath;

    /**
     * 看图猜成语
     * exec ktccy
     */
    public void execKtccy() {
        // 未猜完无法出题
        if (CollUtil.isNotEmpty(CommonGameUtil.GAME_CACHE)) {
            return;
        }
        // 游戏存在无法出题
        Game game = GameListUtil.getGame(tgBot.getGroup());
        if (game != null) {
            return;
        }
        // 非游戏时间
        if (GameUtil.isInNotCommonGameTime()) {
            return;
        }
        // 大于1小时无人回答出题 否则静置
        if (System.currentTimeMillis() -
                CommonGameUtil.endSpeakTime < RandomUtil.randomInt(55 * 60 * 1000, 65 * 60 * 1000)) {
            return;
        }

        GameKtccy ktccy = gameKtccyDao.getRandom2();
        if (ktccy == null) {
            return;
        }
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(tgBot.getGroup()).caption(KTCCY_TIP)
                .photo(new InputFile(new ByteArrayInputStream(
                        HttpUtil.downloadBytes(ktccy.getPicUrl())), "ktccy.jpg"))
                .build();
        tgBot.sendPhoto(sendPhoto);
        CommonGameUtil.GAME_CACHE.put("KTCCY", ktccy.getAnswer());
        log.warn("[成语猜猜看] {}", ktccy.getAnswer());
        gameKtccyDao.upPlayTime(ktccy.getId());
    }

    /**
     * 看图猜成语
     * exec ktccy
     */
    public void execKtcfh() {
        // 未猜完无法出题
        if (CollUtil.isNotEmpty(CommonGameUtil.GAME_CACHE)) {
            return;
        }

        Path path = GameUtil.searchPoster(fhPath);
        if (path == null) {
            return;
        }
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(tgBot.getGroup()).caption(KTCFH_TIP)
                .photo(new InputFile(path.toFile()))
                .hasSpoiler(true)
                .build();
        tgBot.sendPhoto(sendPhoto);
        String filePath = GameUtil.getFhName(path.toString());
        CommonGameUtil.GAME_CACHE.put("KTCFH", filePath);
        log.warn("[道观我最强] {}", filePath);
        // gameKtccyDao.upPlayTime(ktccy.getId());
    }
}