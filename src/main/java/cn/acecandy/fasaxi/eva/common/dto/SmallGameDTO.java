package cn.acecandy.fasaxi.eva.common.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 小游戏 DTO
 *
 * @author tangningzhu
 * @since 2024/11/22
 */
@Builder
@Data
public class SmallGameDTO {
    /**
     * 类型
     */
    private String type;
    /**
     * 答案
     */
    private String answer;
    /**
     * 消息id
     */
    private Integer msgId;
}