package cn.acecandy.fasaxi.eva.common.dto;

import cn.acecandy.fasaxi.eva.common.enums.RedType;
import cn.acecandy.fasaxi.eva.utils.TgUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.acecandy.fasaxi.eva.common.enums.RedType.均等红包;

/**
 * 红包实体类
 *
 * @author tangningzhu
 * @since 2025/7/22
 */
@Data
public class RedDTO {
    private String id;
    /**
     * 红包金额
     */
    private Integer money;
    /**
     * 红包人数
     */
    private Integer members;

    /**
     * 发送人
     */
    private User sendUser;
    /**
     * 红包类型
     */
    private RedType type;

    /**
     * 红包队列
     */
    private final ConcurrentLinkedQueue<Integer> moneyQueue;
    /**
     * 剩余红包数量
     */
    private final AtomicInteger remainingMembers;
    /**
     * 领取人
     */
    private final Map<Long, RedReceiverDTO> receivers = MapUtil.newConcurrentHashMap();

    /**
     * 对应群消息
     */
    private Message msg;

    public RedDTO(String id, Integer money, Integer members, User sendUser, RedType type) {
        this.id = id;
        this.money = money;
        this.members = members;
        this.sendUser = sendUser;
        this.type = type;
        this.remainingMembers = new AtomicInteger(members);

        // 预分配红包金额
        // 打乱顺序
        List<Integer> amounts = 均等红包.equals(type)
                ? generateEqualAmounts(money, members)
                : generateRandomAmounts(money, members);
        Collections.shuffle(amounts);
        this.moneyQueue = new ConcurrentLinkedQueue<>(amounts);
    }

    /**
     * 生成均分红包金额列表
     *
     * @param money   钱
     * @param members 成员
     * @return {@link List }<{@link BigDecimal }>
     */
    private List<Integer> generateEqualAmounts(Integer money, Integer members) {
        List<Integer> amounts = CollUtil.newArrayList();
        Integer amount = money / members;

        for (int i = 0; i < members; i++) {
            amounts.add(amount);
        }

        // 处理精度误差，将余数加到最后一个红包
        Integer remainder = money - amount * members;
        if (remainder > 0) {
            CollUtil.setOrAppend(amounts, members - 1,
                    CollUtil.get(amounts, members - 1) + remainder);
        }
        return amounts;
    }

    /**
     * 生成拼手气红包金额列表（二倍均值算法）
     *
     * @param money   钱
     * @param members 成员
     * @return {@link List }<{@link BigDecimal }>
     */
    private List<Integer> generateRandomAmounts(Integer money, Integer members) {
        List<Integer> amounts = CollUtil.newArrayList();
        int remainingAmount = money;
        int remainingCount = members;

        for (int i = 0; i < members - 1; i++) {
            // 计算当前最大可分配金额（剩余均值的2倍）
            int max = Math.min(remainingAmount - remainingCount + 1, 2 * remainingAmount / remainingCount);

            // 生成随机金额（1到max之间）
            int randomValue = RandomUtil.randomInt(1, max + 1);

            amounts.add(randomValue);
            remainingAmount -= randomValue;
            remainingCount--;
        }

        // 最后一个红包包含所有剩余金额
        amounts.add(remainingAmount);
        return amounts;
    }

    public RedReceiverDTO grab(User user) {
        // 检查是否还有剩余红包
        if (remainingMembers.get() <= 0) {
            return null;
        }

        // 从队列中获取预分配的金额
        Integer amount = moneyQueue.poll();
        if (amount == null) {
            return null;
        }
        // 更新剩余数量
        remainingMembers.decrementAndGet();

        // 记录领取信息
        RedReceiverDTO receiver = new RedReceiverDTO(user, amount);
        receivers.put(user.getId(), receiver);

        return receiver;
    }

    public boolean isEmpty() {
        return remainingMembers.get() <= 0;
    }

    public String getFinalMessage() {
        String startMsg = StrUtil.format("🧧 Dmail红包\n\n😎 {} 的红包已经被瓜分完了~\n\n",
                TgUtil.tgNameOnUrl(sendUser));

        // 找出最大领取金额
        Integer max = receivers.values().stream().map(RedReceiverDTO::getAmount)
                .max(Integer::compareTo).orElse(0);

        // 统计最大金额的领取人数
        long maxCount = receivers.values().stream()
                .filter(receiver -> receiver.getAmount().equals(max))
                .count();

        List<String> tipList = CollUtil.newArrayList();
        boolean isLuckyType = type.equals(RedType.拼手气红包);

        receivers.forEach((userId, receiver) -> {
            int amount = receiver.getAmount();
            String tip;
            if (isLuckyType && amount == max && maxCount == 1) {
                tip = StrUtil.format("<b>🏆[手气最佳] {}</b> 抢到了 {} 点心意",
                        TgUtil.tgNameOnUrl(receiver.getUser()), amount);
            } else {
                tip = StrUtil.format("<b>{}</b> 抢到了 {} 点心意",
                        TgUtil.tgNameOnUrl(receiver.getUser()), amount);
            }
            tipList.add(tip);
        });

        return startMsg + CollUtil.join(tipList, "\n");
    }
}