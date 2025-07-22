package cn.acecandy.fasaxi.eva.task.impl;

import cn.acecandy.fasaxi.eva.common.dto.RedDTO;
import cn.acecandy.fasaxi.eva.common.enums.RedType;
import cn.acecandy.fasaxi.eva.dao.entity.Emby;
import cn.acecandy.fasaxi.eva.dao.service.EmbyDao;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

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

    @Resource
    private EmbyDao embyDao;

    /**
     * 创建红包
     *
     * @param money      钱
     * @param members    成员
     * @param senderId   寄件人身份
     * @param senderName 事件发送者
     * @param redType    红包类型
     * @return {@link String }
     */
    public String createRed(Integer money, Integer members, Long senderId,
                            String senderName, RedType redType) {
        String id = IdUtil.fastSimpleUUID();
        RedDTO envelope = new RedDTO(id, money, members, senderId, senderName, redType);
        RED_ENVELOPES.put(id, envelope);
        return id;
    }

    /**
     * 领取红包
     *
     * @param redId    红色id
     * @param userId   用户id
     * @param userName 用户名
     * @return {@link String }
     */
    public String grabRed(String redId, Long userId, String userName) {
        RedDTO envelope = RED_ENVELOPES.get(redId);
        if (envelope == null || envelope.isEmpty()) {
            return "❌ 你赶到的时候发现地上竟然毛也没有";
        }

        Emby user = embyDao.findByTgId(userId);
        if (user == null) {
            return "❌ 请先私聊bot才能领取哦～";
        }

        if (envelope.getReceivers().containsKey(userId)) {
            return "❌ 住口！你已经领取过红包了！";
        }

        if (null != envelope.grab(userId, userName)) {
            embyDao.upIv(userId, envelope.getReceivers().get(userId).getAmount());

            if (envelope.isEmpty()) {
                RED_ENVELOPES.remove(redId);
            }
            return StrUtil.format("🧧恭喜你从无数人的脚下捡到了\n{} 的 {} 点心意！",
                    envelope.getSenderName(), envelope.getReceivers().get(userId).getAmount());
        } else {
            return "❌ 你赶到的时候发现地上竟然毛也没有";
        }
    }

    public static void main(String[] args) {
        String id = IdUtil.fastSimpleUUID();
        RedDTO envelope = new RedDTO(id, 100, 7,
                111L, "senderName", RedType.拼手气红包);
        RED_ENVELOPES.put(id, envelope);
        Console.log(envelope);
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            int finalI1 = i;
            ThreadUtil.execAsync(() -> {
                Console.log(grabRed2(id, finalI + 1L, "userName" + finalI1));
            });
        }
        Console.log(envelope.getFinalMessage());
    }

    public static String grabRed2(String redId, Long userId, String userName) {
        RedDTO envelope = RED_ENVELOPES.get(redId);
        if (envelope == null || envelope.isEmpty()) {
            return "❌ 你赶到的时候发现地上竟然毛也没有";
        }

        if (envelope.getReceivers().containsKey(userId)) {
            return "❌ 住口！你已经领取过红包了！";
        }

        if (null != envelope.grab(userId, userName)) {
            Console.log("提升:{}", envelope.getReceivers().get(userId).getAmount());

            if (envelope.isEmpty()) {
                RED_ENVELOPES.remove(redId);
            }

            return StrUtil.format("🧧 恭喜，你在万军丛中无数人的脚下捡到了\n {} 的 {} 点心意！",
                    envelope.getSenderName(), envelope.getReceivers().get(userId).getAmount());
        } else {
            return "❌ 你赶到的时候发现地上竟然毛也没有";
        }
    }
}