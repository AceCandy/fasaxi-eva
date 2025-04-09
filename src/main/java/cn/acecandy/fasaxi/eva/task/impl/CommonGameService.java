package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.bot.EmbyTelegramBot;
import cn.acecandy.fasaxi.eva.bot.game.Command;
import cn.acecandy.fasaxi.eva.common.dto.SmallGameDTO;
import cn.acecandy.fasaxi.eva.dao.entity.GameKtccy;
import cn.acecandy.fasaxi.eva.dao.service.GameKtccyDao;
import cn.acecandy.fasaxi.eva.utils.CommonGameUtil;
import cn.acecandy.fasaxi.eva.utils.GameUtil;
import cn.acecandy.fasaxi.eva.utils.ImgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

import static cn.acecandy.fasaxi.eva.bot.game.Command.看图猜成语;
import static cn.acecandy.fasaxi.eva.bot.game.Command.看图猜番号;
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

    @Value("${game.ccy-path}")
    private String ccyPath;

    /**
     * 看图猜成语
     */
    @Transactional(rollbackFor = Exception.class)
    public void execKtccy() {
        // 未猜完无法出题
        if (CollUtil.isNotEmpty(CommonGameUtil.GAME_CACHE)) {
            return;
        }
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

    public void ktccy() {
        GameKtccy ktccy = gameKtccyDao.getRandom2();
        if (ktccy == null) {
            return;
        }

        gameKtccyDao.upPlayTime(ktccy.getId());
        File picFile = null;
        if (StrUtil.isBlank(ktccy.getFileUrl())) {
            try {
                picFile = HttpUtil.downloadFileFromUrl(ktccy.getPicUrl(),
                        FileUtil.mkdir(ccyPath + ktccy.getSource()));
            } catch (Exception e) {
                if (StrUtil.containsIgnoreCase(ktccy.getPicUrl(), "https://free.wqwlkj.cn/wqwlapi/data/")) {
                    ktccy.setPicUrl(ktccy.getPicUrl().replace("https://free.wqwlkj.cn/wqwlapi/data/",
                            "https://api.lolimi.cn/API/ktcc/"));
                    log.info("远程获取url:{}", ktccy.getPicUrl());
                    picFile = HttpUtil.downloadFileFromUrl(ktccy.getPicUrl(),
                            FileUtil.mkdir(ccyPath + ktccy.getSource()));
                } else {
                    throw new RuntimeException(StrUtil.format("图片下载失败: {}", ktccy.getPicUrl()), e);
                }
            }
            gameKtccyDao.updateFileUrl(ktccy.getId(), picFile.getAbsolutePath());
        } else {
            picFile = FileUtil.file(ktccy.getFileUrl());
        }
        // 简笔化
        String handleFileUrl = StrUtil.replaceLast(ktccy.getFileUrl(),
                ".jpg", "-briefStrokes.jpg");
        if (FileUtil.exist(handleFileUrl)) {
            picFile = FileUtil.file(handleFileUrl);
        } else {
            picFile = ImgUtil.briefStrokes(picFile);
        }

        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(tgBot.getGroup()).caption(KTCCY_TIP)
                .photo(new InputFile(picFile))
                .build();
        Message msg = tgBot.sendPhoto(sendPhoto, 60 * 60 * 1000, 看图猜成语);
        CommonGameUtil.GAME_CACHE.offer(SmallGameDTO.builder()
                .type(看图猜成语).answer(ktccy.getAnswer()).msgId(msg.getMessageId()).build());
        log.warn("[成语猜猜看] {}", ktccy.getAnswer());
    }

    /**
     * 看图猜番号
     */
    @SneakyThrows
    public void execKtcfh() {
        // 未猜完无法出题
        // if (CollUtil.isNotEmpty(CommonGameUtil.GAME_CACHE)) {
        //     return;
        // }

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
            Message msg = tgBot.sendPhoto(sendPhoto, 55 * 60 * 1000);
            String fhName = GameUtil.getFhName(path.toString());
            CommonGameUtil.GAME_CACHE.offer(SmallGameDTO.builder()
                    .type(看图猜番号).answer(fhName).msgId(msg.getMessageId()).build());
            log.warn("[道观我最强] {}", fhName);
        }
    }

    public static void main(String[] args) {
        FileUtil.mkdir("/tmp/ktccy/" + 2);
        HttpUtil.downloadFileFromUrl("http://hm.suol.cc/API/ktccy/img/425.jpg",
                FileUtil.mkdir("/tmp/ktccy/" + 3));
    }
}