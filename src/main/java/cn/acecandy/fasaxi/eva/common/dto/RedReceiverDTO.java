package cn.acecandy.fasaxi.eva.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

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
    private Long userId;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 红包金额
     */
    private Integer amount;
}