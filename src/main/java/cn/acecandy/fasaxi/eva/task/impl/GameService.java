package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.common.dto.SmallGameDTO;
import cn.acecandy.fasaxi.eva.config.CommonGameConfig;
import cn.acecandy.fasaxi.eva.dao.entity.GameKtccy;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.dao.service.GameKtccyDao;
import cn.acecandy.fasaxi.eva.utils.FhUtil;
import cn.acecandy.fasaxi.eva.utils.GameUtil;
import cn.acecandy.fasaxi.eva.utils.GlobalUtil;
import cn.acecandy.fasaxi.eva.utils.ImgUtil;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.acecandy.fasaxi.eva.utils.WdUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static cn.acecandy.fasaxi.eva.bot.game.Command.看图猜成语;
import static cn.acecandy.fasaxi.eva.bot.game.Command.看图猜番号;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.COMMON_WIN;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.KTCCY_TIP;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.KTCFH_TIP;
import static cn.acecandy.fasaxi.eva.utils.GlobalUtil.GAME_SPEAK_CNT;

/**
 * 通用定时任务 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class GameService {

    @Resource
    private TgService tgService;

    @Resource
    private GameKtccyDao gameKtccyDao;

    @Resource
    private CommonGameConfig commonGameConfig;

    @Resource
    private EmbyDao embyDao;

    /**
     * 看图猜成语
     */
    @Transactional(rollbackFor = Exception.class)
    public void execKtccy() {
        // 未猜完无法出题
        if (CollUtil.isNotEmpty(GameUtil.GAME_CACHE)) {
            return;
        }
        // 非游戏时间
        if (WdUtil.isInNotCommonGameTime()) {
            return;
        }
        // 大于50min无人回答出题 否则静置
        if (System.currentTimeMillis() -
                GlobalUtil.lastSpeakTime > RandomUtil.randomInt(105, 110) * 60 * 1000) {
            ktccy();
            GlobalUtil.lastSpeakTime = System.currentTimeMillis();
        }
        if (GAME_SPEAK_CNT.get() <= -200) {
            ktccy();
            GAME_SPEAK_CNT.set(RandomUtil.randomInt(40, 60));
        }

    }

    public void ktccy() {
        GameKtccy ktccy = gameKtccyDao.getRandom2();
        if (ktccy == null) {
            return;
        }
        String defaultPath = commonGameConfig.getKtccy().getPath();

        gameKtccyDao.upPlayTime(ktccy.getId());
        File picFile = null;
        if (StrUtil.isBlank(ktccy.getFileUrl())) {
            try {
                picFile = HttpUtil.downloadFileFromUrl(ktccy.getPicUrl(),
                        FileUtil.mkdir(defaultPath + ktccy.getSource()));
            } catch (Exception e) {
                if (StrUtil.containsIgnoreCase(ktccy.getPicUrl(), "https://free.wqwlkj.cn/wqwlapi/data/")) {
                    ktccy.setPicUrl(ktccy.getPicUrl().replace("https://free.wqwlkj.cn/wqwlapi/data/",
                            "https://api.lolimi.cn/API/ktcc/"));
                    log.info("远程获取url:{}", ktccy.getPicUrl());
                    picFile = HttpUtil.downloadFileFromUrl(ktccy.getPicUrl(),
                            FileUtil.mkdir(defaultPath + ktccy.getSource()));
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

        SmallGameDTO smallGame = SmallGameDTO.builder().type(看图猜成语).answer(ktccy.getAnswer()).build();
        try {
            SendPhoto sendPhoto = SendPhoto.builder()
                    .chatId(tgService.getGroup()).caption(KTCCY_TIP)
                    .photo(new InputFile(picFile))
                    .build();
            Message msg = tgService.sendPhoto(sendPhoto, 60 * 60 * 1000, 看图猜成语);
            smallGame.setMsgId(msg.getMessageId());
        } finally {
            log.warn("[成语猜猜看] {}", ktccy.getAnswer());
            GameUtil.GAME_CACHE.offer(smallGame);
        }

    }

    public void ktccyToDb() {
        List<GameKtccy> ktccyList = gameKtccyDao.findAllNoFileUrl();
        if (CollUtil.isEmpty(ktccyList)) {
            return;
        }
        String defaultPath = commonGameConfig.getKtccy().getPath();
        ktccyList.forEach(ktccy -> {
            File picFile = null;
            try {
                picFile = HttpUtil.downloadFileFromUrl(ktccy.getPicUrl(),
                        FileUtil.mkdir(defaultPath + ktccy.getSource()));
            } catch (Exception e) {
                if (StrUtil.containsIgnoreCase(ktccy.getPicUrl(), "https://free.wqwlkj.cn/wqwlapi/data/")) {
                    ktccy.setPicUrl(ktccy.getPicUrl().replace("https://free.wqwlkj.cn/wqwlapi/data/",
                            "https://api.lolimi.cn/API/ktcc/"));
                    log.info("远程获取url:{}", ktccy.getPicUrl());
                    picFile = HttpUtil.downloadFileFromUrl(ktccy.getPicUrl(),
                            FileUtil.mkdir(defaultPath + ktccy.getSource()));
                } else {
                    log.error("====={}[{}] 下载失败", ktccy.getAnswer(), ktccy.getPicUrl());
                    return;
                }
            }
            gameKtccyDao.updateFileUrl(ktccy.getId(), picFile.getAbsolutePath());

            // 简笔化
            String handleFileUrl = StrUtil.replaceLast(ktccy.getFileUrl(),
                    ".jpg", "-briefStrokes.jpg");
            if (!FileUtil.exist(handleFileUrl)) {
                ImgUtil.briefStrokes(picFile);
            }
            log.info("{}[{}] 下载成功", ktccy.getAnswer(), ktccy.getPicUrl());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void execKtcfh() {
        // 非游戏时间
        if (!WdUtil.isInNotCommonGameTime()) {
            return;
        }
        // 大于50min无人回答出题 否则静置
        if (System.currentTimeMillis() -
                GlobalUtil.lastSpeakTime > RandomUtil.randomInt(105, 110) * 60 * 1000) {
            ktcfh();
            GlobalUtil.lastSpeakTime = System.currentTimeMillis();
        }
        if (GAME_SPEAK_CNT.get() <= -200) {
            ktcfh();
            GAME_SPEAK_CNT.set(RandomUtil.randomInt(40, 60));
        }

    }

    /**
     * 看图猜番号
     */
    @SneakyThrows
    public void ktcfh() {
        String defaultPath = commonGameConfig.getKtcfh().getPath();

        Path path = FhUtil.searchPoster(defaultPath);
        if (path == null) {
            return;
        }
        String fhName = FhUtil.getFhName(path.toString());
        SmallGameDTO smallGame = SmallGameDTO.builder().type(看图猜番号)
                .answer(fhName).build();
        try (InputStream input = ImgUtil.protectPic(path.toFile())) {
            if (null == input) {
                return;
            }
            SendPhoto sendPhoto = SendPhoto.builder()
                    .chatId(tgService.getGroup()).caption(KTCFH_TIP)
                    .photo(new InputFile(input, "ktcfh.jpg")).hasSpoiler(true).build();
            Message msg = tgService.sendPhoto(sendPhoto, 55 * 60 * 1000);
            smallGame.setMsgId(msg.getMessageId());
        } finally {
            log.warn("[道观我最强] {}", fhName);
            GameUtil.GAME_CACHE.offer(smallGame);
        }
    }

    /**
     * 发言答题
     *
     * @param message 消息
     */
    public void speak(Message message) {
        String text = message.getText();
        if (!StrUtil.startWith(text, "。")) {
            return;
        }
        text = StrUtil.removeAllPrefix(text, "。");
        SmallGameDTO smallGame = GameUtil.checkAnswer(text);
        if (null == smallGame) {
            return;
        }
        int lv = GameUtil.getGameRewards(smallGame.getType());
        commonWin(message, lv);
        tgService.delMsg(message.getChatId().toString(), smallGame.getMsgId());
    }

    /**
     * 用于通用游戏 获取奖励
     *
     * @param message 消息
     * @param lv      胜利奖励
     */
    private void commonWin(Message message, Integer lv) {
        if (lv == null || lv < 1) {
            return;
        }
        tgService.sendMsg(message.getMessageId(), message.getChatId().toString(),
                StrUtil.format(COMMON_WIN, TgUtil.tgNameOnUrl(message.getFrom()), lv));
        embyDao.upIv(message.getFrom().getId(), lv);
    }

    public static void main(String[] args) {
        FileUtil.mkdir("/tmp/ktccy/" + 2);
        HttpUtil.downloadFileFromUrl("http://hm.suol.cc/API/ktccy/img/425.jpg",
                FileUtil.mkdir("/tmp/ktccy/" + 3));
    }
}