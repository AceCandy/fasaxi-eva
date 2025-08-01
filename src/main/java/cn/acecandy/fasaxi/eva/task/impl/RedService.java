package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.common.dto.RedDTO;
import cn.acecandy.fasaxi.eva.common.enums.RedType;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Map;

import static cn.acecandy.fasaxi.eva.common.constants.GameTextConstants.TIP_IN_GROUP;

/**
 * @author tangningzhu
 * @since 2025/7/22
 */
@Slf4j
@Component
public class RedService {
    /**
     * 红包
     */
    private static final Map<String, RedDTO> RED_ENVELOPES = MapUtil.newConcurrentHashMap();

    /**
     * 获取红包
     *
     * @param redId 红色id
     * @return {@link RedDTO }
     */
    public RedDTO getRedEnvelope(String redId) {
        return RED_ENVELOPES.get(redId);
    }

    /**
     * 获取红包
     *
     * @param redId 红色id
     * @return {@link RedDTO }
     */
    public RedDTO removeRedEnvelope(String redId) {
        return RED_ENVELOPES.remove(redId);
    }

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
        String msgText = StrUtil.trim(StrUtil.removePrefix(message.getText(), "/red"));
        handleRedCommand(chatId, message.getFrom(), msgText);
    }

    @SneakyThrows
    private void handleRedCommand(String chatId, User user, String text) {
        Long userId = user.getId();
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
        redDTO.setMsg(tgService.sendPhoto(msg));
    }

    public Emby isEmbyUser(String chatId, Long userId) {
        Emby embyUser = embyDao.findByTgId(userId);
        if (embyUser == null) {
            tgService.sendMsg(chatId, "您还未在bot处登记哦~", 5 * 1000);
        }
        return embyUser;
    }

    /**
     * 创建红包
     *
     * @param money   钱
     * @param members 成员
     * @param redType 红包类型
     * @return {@link String }
     */
    public RedDTO createRed(Integer money, Integer members, User sendUser, RedType redType) {
        String id = IdUtil.fastSimpleUUID();
        RedDTO envelope = new RedDTO(id, money, members, sendUser, redType);
        RED_ENVELOPES.put(id, envelope);
        return envelope;
    }

    /**
     * 领取红包
     *
     * @param redId 红色id
     * @param user  用户
     * @return {@link String }
     */
    public String grabRed(String redId, User user) {
        RedDTO envelope = getRedEnvelope(redId);
        if (envelope == null || envelope.isEmpty()) {
            return "❌ 你赶到的时候发现地上竟然毛也没有";
        }
        Long userId = user.getId();
        Emby embyUser = embyDao.findByTgId(userId);
        if (embyUser == null) {
            return "❌ 请先私聊bot！！";
        }

        if (envelope.getReceivers().containsKey(userId)) {
            return "❌ 住口！你已经领取过红包了！";
        }

        if (null != envelope.grab(user)) {
            embyDao.upIv(userId, envelope.getReceivers().get(userId).getAmount());
            return StrUtil.format("🧧一顿猛抢，你终于捡到了\n {} 的 {} 点心意！",
                    envelope.getSendUser().getFirstName(), envelope.getReceivers().get(userId).getAmount());
        } else {
            return "❌ 你赶到的时候发现地上竟然毛也没有";
        }
    }

}