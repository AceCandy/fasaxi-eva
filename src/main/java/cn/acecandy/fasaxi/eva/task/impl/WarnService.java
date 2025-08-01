package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TIP_IN_CMD_ERROR;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TIP_IN_GROUP;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TIP_IN_OWNER;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.YELLOW_PAI;

/**
 * 警告相关service
 *
 * @author tangningzhu
 * @since 2025/7/22
 */
@Slf4j
@Component
public class WarnService {

    @Resource
    private EmbyDao embyDao;
    @Resource
    private TgService tgService;

    public void process(String cmd, Message message) {
        String chatId = message.getChatId().toString();
        if (!TgUtil.isGroupMsg(message)) {
            tgService.sendMsg(chatId, TIP_IN_GROUP, 10 * 1000);
            return;
        }
        User user = message.getFrom();
        Long userId = user.getId();
        if (!CollUtil.contains(tgService.getAdmins(), userId)) {
            tgService.sendMsg(userId.toString(), TIP_IN_OWNER, 5 * 1000);
            return;
        }
        String msgText = StrUtil.trim(StrUtil.removePrefix(message.getText(), "/fp"));
        handleWarnCommand(chatId, message.getReplyToMessage(), msgText);
    }

    @SneakyThrows
    private void handleWarnCommand(String chatId, Message msg, String text) {
        if (null == msg) {
            tgService.sendMsg(chatId, TIP_IN_CMD_ERROR, 5 * 1000);
            return;
        }
        User user = msg.getFrom();
        if (StrUtil.equalsIgnoreCase(text, "1")) {
            embyDao.upIv(user.getId(), -100);
            SendPhoto sendPhoto = SendPhoto.builder()
                    .chatId(tgService.getGroup()).caption(YELLOW_PAI)
                    .replyToMessageId(msg.getMessageId())
                    .photo(new InputFile(ResourceUtil.getStream("static/pic/黄牌警告.jpeg"), "黄牌警告"))
                    .build();
            tgService.sendPhoto(sendPhoto, 60 * 1000);
        } else {
            tgService.sendMsg(chatId, TIP_IN_CMD_ERROR, 5 * 1000);
        }
    }

}