package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TIP_IN_GROUP;
import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TIP_IN_OWNER;

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
        String msgText = StrUtil.trim(StrUtil.removePrefix(message.getText(), "/warn"));
        handleWarnCommand(chatId, message.getReplyToMessage().getFrom(), msgText);
    }

    @SneakyThrows
    private void handleWarnCommand(String chatId, User user, String text) {
        /*Long userId = user.getId();
        Emby emby = isEmbyUser(chatId, userId);
        if (null == emby) {
            return;
        }
        Integer total, members, mode = null;
        try {
            int[] params = StrUtil.splitToInt(text, " ");
            if (params.length < 2) {
                throw new RuntimeException("参数错误");
            }
            total = params[0];
            members = params[1];
            if (params.length > 2) {
                mode = params[2];
            }
        } catch (Exception e) {
            tgService.sendMsg(chatId, "❌ 格式错误，参考格式：/red [总数] [份数] [mode]\n" +
                    "默认拼手气 mode为任意数字为均分红包", 10 * 1000);
            return;
        }
        if (emby.getIv() < total) {
            tgService.sendMsg(chatId, "❌ 这么穷学别人发什么红包？", 5 * 1000);
            return;
        }
        if (members > 20) {
            tgService.sendMsg(chatId, "❌ 无法发送这么多数量的红包", 5 * 1000);
            return;
        }
        if (members < 3) {
            tgService.sendMsg(chatId, "❌ 无法发送这么少数量的红包", 5 * 1000);
            return;
        }
        embyDao.upIv(userId, -total);

        int cost = total;
        // 税收
        if (total > 500 && members < 5) {
            cost = (int) (total / 1.5);
        } else if (total > 200 && members < 5) {
            cost = (int) (total / 1.35);
        } else if (total > 50 && members < 5) {
            cost = (int) (total / 1.2);
        }
        RedDTO redDTO = createRed(cost, members, user, null == mode ? RedType.拼手气红包 : RedType.均等红包);
        SendPhoto msg = SendPhoto.builder()
                .chatId(chatId).photo(new InputFile(
                        ResourceUtil.getStream("static/pic/红包.png"), "红包.png"))
                .replyMarkup(TgUtil.getRedBtn(redDTO.getId()))
                .build();
        redDTO.setMsg(tgService.sendPhoto(msg));*/
    }

}