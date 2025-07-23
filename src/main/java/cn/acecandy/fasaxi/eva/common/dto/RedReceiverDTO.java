package cn.acecandy.fasaxi.eva.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.User;

/**
 * @author tangningzhu
 * @since 2025/7/22
 */
@AllArgsConstructor
@Data
public class RedReceiverDTO {
    /**
     * 用户id
     */
    private User user;
    /**
     * 红包金额
     */
    private Integer amount;
}