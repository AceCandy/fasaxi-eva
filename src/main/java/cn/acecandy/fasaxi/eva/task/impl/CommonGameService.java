package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.bot.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.bot.game.Command;
import cn.acecandy.fasaxi.eva.dao.entity.GameKtccy;
import cn.acecandy.fasaxi.eva.dao.service.GameKtccyDao;
import cn.acecandy.fasaxi.eva.utils.CommonGameUtil;
import cn.acecandy.fasaxi.eva.utils.GameUtil;
import cn.acecandy.fasaxi.eva.utils.ImgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
        /*Game game = GameListUtil.getGame(tgBot.getGroup());
        if (game != null) {
            return;
        }*/
        // 非游戏时间
        if (GameUtil.isInNotCommonGameTime()) {
            return;
        }
        // 大于50min无人回答出题 否则静置
        if (System.currentTimeMillis() -
                CommonGameUtil.endSpeakTime > RandomUtil.randomInt(105, 110) * 60 * 1000) {
            ktccy();
            CommonGameUtil.endSpeakTime = System.currentTimeMillis();
        }
        if (Command.SPEAK_TIME_CNT.get() <= -200) {
            ktccy();
            Command.SPEAK_TIME_CNT.set(RandomUtil.randomInt(40, 60));
        }

    }

    private void ktccy() {
        GameKtccy ktccy = gameKtccyDao.getRandom2();
        if (ktccy == null) {
            return;
        }
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(tgBot.getGroup()).caption(KTCCY_TIP)
                .photo(new InputFile(new ByteArrayInputStream(
                        HttpUtil.downloadBytes(ktccy.getPicUrl())), "ktccy.png"))
                .build();
        tgBot.sendPhoto(sendPhoto, 60 * 60 * 1000);
        CommonGameUtil.GAME_CACHE.put("KTCCY", ktccy.getAnswer());
        log.warn("[成语猜猜看] {}", ktccy.getAnswer());
        gameKtccyDao.upPlayTime(ktccy.getId());
    }

    /**
     * 看图猜成语
     * exec ktccy
     */
    @SneakyThrows
    public void execKtcfh() {
        // 未猜完无法出题
        if (CollUtil.isNotEmpty(CommonGameUtil.GAME_CACHE)) {
            return;
        }

        Path path = GameUtil.searchPoster(fhPath);
        if (path == null) {
            return;
        }
        try (InputStream input = ImgUtil.protectPic(path.toFile())) {
            if (null == input) {
                return;
            }
            SendPhoto sendPhoto = SendPhoto.builder()
                    .chatId(tgBot.getGroup()).caption(KTCFH_TIP)
                    .photo(new InputFile(input, "ktcfh.jpg")).hasSpoiler(true).build();
            tgBot.sendPhoto(sendPhoto, 55 * 60 * 1000);
            String filePath = GameUtil.getFhName(path.toString());
            CommonGameUtil.GAME_CACHE.put("KTCFH", filePath);
            log.warn("[道观我最强] {}", filePath);
        }
    }
}